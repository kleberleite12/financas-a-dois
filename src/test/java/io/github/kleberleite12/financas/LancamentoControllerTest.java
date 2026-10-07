package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LancamentoControllerTest extends WebIntegrationTest {
    static Stream<Arguments> activeTypesAndUsers() {
        return Stream.of("kleber", "giovanna").flatMap(user -> Stream.of(
                TipoLancamento.RENDA_PRINCIPAL, TipoLancamento.RENDA_EXTRA,
                TipoLancamento.CARTAO_CREDITO, TipoLancamento.OUTRO_GASTO, TipoLancamento.GUARDADO)
                .map(type -> Arguments.of(user, type)));
    }

    @ParameterizedTest
    @MethodSource("activeTypesAndUsers")
    void createsEachActiveTypeAndIgnoresForgedOwnerAndDate(String actor, TipoLancamento type) throws Exception {
        mvc.perform(formPost(actor, Map.of("tipo", type.name(), "valor", "1234.56",
                        "periodoLancamento", "2026-12", "responsavel", "Outra pessoa", "data", "2020-01-01")))
                .andExpect(redirectedUrl("/lancamentos"));
        var persisted = repository.findAll();
        assertThat(persisted).hasSize(1);
        var entry = persisted.getFirst();
        assertThat(entry.getId()).isPositive();
        assertThat(entry.getValor()).isEqualByComparingTo("1234.56");
        assertThat(entry.getTipo()).isEqualTo(type);
        assertThat(entry.getData()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(entry.getDescricao()).isEqualTo("Descrição teste");
        assertThat(entry.getCategoria()).isEqualTo("Categoria teste");
        assertThat(entry.getResponsavel()).isEqualTo(actor.equals("kleber") ? "Kleber" : "Giovanna");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RENDA_PRINCIPAL", "RENDA_EXTRA", "CARTAO_CREDITO", "OUTRO_GASTO", "GUARDADO"})
    void blankOptionalFieldsUseTypeDescription(String type) throws Exception {
        mvc.perform(formPost("kleber", Map.of("tipo", type, "descricao", "  ", "categoria", "")))
                .andExpect(redirectedUrl("/lancamentos"));
        var entry = repository.findAll().getFirst();
        assertThat(entry.getDescricao()).isEqualTo(TipoLancamento.valueOf(type).getDescricao());
        assertThat(entry.getCategoria()).isEqualTo(TipoLancamento.valueOf(type).getDescricao());
    }

    @Test
    void absentOptionalFieldsUseTypeDescription() throws Exception {
        var fields = new HashMap<String, String>();
        fields.put("descricao", null); fields.put("categoria", null);
        mvc.perform(formPost("kleber", fields)).andExpect(redirectedUrl("/lancamentos"));
        var entry = repository.findAll().getFirst();
        assertThat(entry.getDescricao()).isEqualTo("Renda extra");
        assertThat(entry.getCategoria()).isEqualTo("Renda extra");
    }

    @Test
    void trimsOptionalFields() throws Exception {
        mvc.perform(formPost("kleber", Map.of("descricao", "  Descrição  ", "categoria", "  Categoria  ")))
                .andExpect(redirectedUrl("/lancamentos"));
        var entry = repository.findAll().getFirst();
        assertThat(entry.getDescricao()).isEqualTo("Descrição");
        assertThat(entry.getCategoria()).isEqualTo("Categoria");
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("valor", null, "Informe o valor do lançamento."),
                Arguments.of("valor", "0", "O valor deve ser maior que zero."),
                Arguments.of("valor", "-0.01", "O valor deve ser maior que zero."),
                Arguments.of("valor", "100000000.00", "O valor informado é muito alto."),
                Arguments.of("valor", "1.001", "O valor pode ter no máximo duas casas decimais."),
                Arguments.of("tipo", null, "Selecione o tipo do lançamento."),
                Arguments.of("tipo", "", "Selecione o tipo do lançamento."),
                Arguments.of("tipo", "RECEITA", "Tipo de lançamento inválido."),
                Arguments.of("tipo", "GASTO", "Tipo de lançamento inválido."),
                Arguments.of("periodoLancamento", null, "Informe o mês e o ano."),
                Arguments.of("periodoLancamento", "", "Informe o mês e o ano."),
                Arguments.of("periodoLancamento", "  ", "Informe o mês e o ano."),
                Arguments.of("periodoLancamento", "2026-13", "Mês e ano inválidos."),
                Arguments.of("periodoLancamento", "2026-00", "Mês e ano inválidos."),
                Arguments.of("periodoLancamento", "11/2026", "Mês e ano inválidos."),
                Arguments.of("descricao", "x".repeat(256), "A descrição pode ter no máximo 255 caracteres."),
                Arguments.of("categoria", "x".repeat(256), "A categoria pode ter no máximo 255 caracteres."));
    }

    @ParameterizedTest(name = "Validação backend #{index}: {0}")
    @MethodSource("invalidFields")
    void serverRejectsInvalidFieldsWithoutPersistence(String field, String value, String message) throws Exception {
        var fields = new HashMap<String, String>(); fields.put(field, value);
        mvc.perform(formPost("kleber", fields)).andExpect(status().isOk())
                .andExpect(view().name("novo-lancamento")).andExpect(model().attribute("mensagemErro", message));
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"tipo,NAO_EXISTE", "valor,abc", "id,abc", "data,nao-e-data"})
    void invalidBindingIsBadRequestWithoutPersistence(String field, String value) throws Exception {
        mvc.perform(formPost("kleber", Map.of(field, value))).andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "99999999.99", "50.000"})
    void acceptsValueBoundariesAndInsignificantTrailingZeros(String amount) throws Exception {
        mvc.perform(formPost("kleber", Map.of("valor", amount, "descricao", "x".repeat(255),
                        "categoria", "y".repeat(255))))
                .andExpect(redirectedUrl("/lancamentos"));
        var entry = repository.findAll().getFirst();
        assertThat(entry.getValor()).isEqualByComparingTo(amount);
        assertThat(entry.getDescricao()).hasSize(255);
        assertThat(entry.getCategoria()).hasSize(255);
    }

    @ParameterizedTest
    @CsvSource({"kleber,Kleber", "giovanna,Giovanna"})
    void editsAllFieldsWithoutChangingIdOwnerOrCreatingDuplicate(String actor, String owner) throws Exception {
        var entry = entry(owner, "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "3000.00");
        mvc.perform(get("/lancamentos/editar/{id}", entry.getId()).with(user(actor)))
                .andExpect(status().isOk()).andExpect(model().attribute("modoEdicao", true));
        mvc.perform(formPost(actor, Map.of("id", entry.getId().toString(), "valor", "800.25",
                        "tipo", "GUARDADO", "periodoLancamento", "2026-12", "descricao", "Editada",
                        "categoria", "Reserva", "responsavel", "Pessoa forjada")))
                .andExpect(redirectedUrl("/lancamentos"));
        assertThat(repository.count()).isEqualTo(1);
        var edited = repository.findById(entry.getId()).orElseThrow();
        assertThat(edited.getId()).isEqualTo(entry.getId());
        assertThat(edited.getResponsavel()).isEqualTo(owner);
        assertThat(edited.getValor()).isEqualByComparingTo("800.25");
        assertThat(edited.getData()).isEqualTo("2026-12-01");
        assertThat(edited.getTipo()).isEqualTo(TipoLancamento.GUARDADO);
        assertThat(edited.getDescricao()).isEqualTo("Editada");
        assertThat(edited.getCategoria()).isEqualTo("Reserva");
        money(dashboard(actor, 2026, 11), "minhasReceitas", "0,00");
        var december = dashboard(actor, 2026, 12);
        money(december, "meuGuardadoMes", "800,25");
        money(december, "meuDisponivelAgora", "-800,25");
    }

    @Test
    void invalidEditPreservesExistingRecord() throws Exception {
        var entry = entry("Kleber", "2026-11-01", TipoLancamento.RENDA_EXTRA, "50.00");
        mvc.perform(formPost("kleber", Map.of("id", entry.getId().toString(), "valor", "-1",
                        "periodoLancamento", "2026-12")))
                .andExpect(view().name("novo-lancamento"))
                .andExpect(model().attribute("mensagemErro", notNullValue()))
                .andExpect(model().attribute("modoEdicao", true))
                .andExpect(model().attribute("periodoLancamento", "2026-12"));
        var unchanged = repository.findById(entry.getId()).orElseThrow();
        assertThat(unchanged.getValor()).isEqualByComparingTo("50.00");
        assertThat(unchanged.getData()).isEqualTo(entry.getData());
        assertThat(repository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"kleber,Kleber", "giovanna,Giovanna"})
    void deletesOwnEntryAndRecalculatesSavingsGoalAndBalance(String actor, String owner) throws Exception {
        entry(owner, "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "1000.00");
        var saved = entry(owner, "2026-11-02", TipoLancamento.GUARDADO, "200.00");
        money(dashboard(actor, 2026, 11), "meuDisponivelAgora", "800,00");
        mvc.perform(post("/lancamentos/excluir/{id}", saved.getId()).with(user(actor)).with(csrf()))
                .andExpect(redirectedUrl("/lancamentos"));
        assertThat(repository.existsById(saved.getId())).isFalse();
        var home = dashboard(actor, 2026, 11);
        money(home, "meuDisponivelAgora", "1.000,00");
        money(home, "meuGuardadoMes", "0,00");
        money(home, actor + "GuardadoAcumulado", "0,00");
        assertThat(home.get(actor + "PercentualMeta")).isEqualTo(new java.math.BigDecimal("0.0"));
    }

    @Test
    void deletingExpenseReducesCategoryTotalAndIncreasesAvailableBalance() throws Exception {
        entry("Kleber", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "1000.00");
        var expense = entry("Kleber", "2026-11-02", TipoLancamento.CARTAO_CREDITO, "200.00");
        mvc.perform(post("/lancamentos/excluir/{id}", expense.getId()).with(user("kleber")).with(csrf()))
                .andExpect(redirectedUrl("/lancamentos"));
        var home = dashboard("kleber", 2026, 11);
        money(home, "kleberCartaoCredito", "0,00");
        money(home, "meusGastos", "0,00");
        money(home, "meuDisponivelAgora", "1.000,00");
    }

    @Test
    void unknownIdsReturnNotFoundForEditSaveAndDelete() throws Exception {
        mvc.perform(get("/lancamentos/editar/999999").with(user("kleber"))).andExpect(status().isNotFound());
        mvc.perform(validPost("kleber").param("id", "999999")).andExpect(status().isNotFound());
        mvc.perform(post("/lancamentos/excluir/999999").with(user("kleber")).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"RECEITA,RENDA_PRINCIPAL", "GASTO,CARTAO_CREDITO"})
    void legacyEditFormMapsTypeWithoutWritingUntilSave(String oldType, String activeType) throws Exception {
        var entry = entry("Kleber", "2026-11-01", TipoLancamento.valueOf(oldType), "50.00");
        var result = mvc.perform(get("/lancamentos/editar/{id}", entry.getId()).with(user("kleber")))
                .andExpect(status().isOk()).andReturn();
        var form = (io.github.kleberleite12.financas.model.Lancamento) viewModel(result).get("lancamento");
        assertThat(form.getTipo()).isEqualTo(TipoLancamento.valueOf(activeType));
        assertThat(repository.findById(entry.getId()).orElseThrow().getTipo()).isEqualTo(TipoLancamento.valueOf(oldType));
        mvc.perform(formPost("kleber", Map.of("id", entry.getId().toString(), "tipo", activeType)))
                .andExpect(redirectedUrl("/lancamentos"));
        assertThat(repository.findById(entry.getId()).orElseThrow().getTipo()).isEqualTo(TipoLancamento.valueOf(activeType));
    }
}
