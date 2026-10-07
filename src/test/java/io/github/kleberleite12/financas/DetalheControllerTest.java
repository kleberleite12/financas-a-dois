package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DetalheControllerTest extends WebIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"RENDA_PRINCIPAL", "RENDA_EXTRA", "CARTAO_CREDITO", "OUTRO_GASTO", "GUARDADO"})
    void cardsRespectOwnerMonthAndTypeWithExactTotal(String type) throws Exception {
        var selected = TipoLancamento.valueOf(type);
        var first = entry("Kleber", "2026-11-01", selected, "50.00");
        var second = entry("Kleber", "2026-11-30", selected, "600.00");
        entry("Kleber", "2026-12-01", selected, "999.00");
        entry("Giovanna", "2026-11-01", selected, "999.00");
        entry("Kleber", "2026-11-01", selected == TipoLancamento.GUARDADO
                ? TipoLancamento.RENDA_EXTRA : TipoLancamento.GUARDADO, "999.00");
        var result = viewModel(mvc.perform(get("/detalhes").with(user("kleber"))
                        .param("periodo", "2026-11").param("responsavel", "Kleber").param("tipo", type))
                .andExpect(status().isOk()).andExpect(view().name("detalhes")).andReturn());
        var entries = entries(result, "lancamentos");
        assertThat(entries).extracting(e -> e.getId()).containsExactly(second.getId(), first.getId());
        assertThat(entries.stream().map(e -> e.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("650.00");
        money(result, "total", "650,00");
        assertThat(result).containsEntry("titulo", selected.getDescricao()).containsEntry("nomeMes", "Novembro")
                .containsEntry("ano", 2026).containsEntry("anoVoltar", 2026).containsEntry("mesVoltar", 11);
    }

    @ParameterizedTest
    @CsvSource({"RECEITA,RENDA_PRINCIPAL", "GASTO,CARTAO_CREDITO"})
    void legacyRecordsAppearInEquivalentCard(String legacy, String active) throws Exception {
        entry("Kleber", "2026-11-01", TipoLancamento.valueOf(legacy), "100.00");
        entry("Kleber", "2026-11-02", TipoLancamento.valueOf(active), "50.00");
        var result = viewModel(mvc.perform(get("/detalhes").with(user("kleber"))
                .param("periodo", "2026-11").param("responsavel", "Kleber").param("tipo", active)).andReturn());
        assertThat(entries(result, "lancamentos")).hasSize(2);
        money(result, "total", "150,00");
    }

    @Test
    void authenticatedUserMayReadOtherPersonsCardCaseInsensitively() throws Exception {
        entry("Giovanna", "2026-12-01", TipoLancamento.GUARDADO, "500.00");
        var result = viewModel(mvc.perform(get("/detalhes").with(user("kleber"))
                .param("periodo", "2026-12").param("responsavel", "gIoVaNnA").param("tipo", "GUARDADO"))
                .andExpect(status().isOk()).andReturn());
        assertThat(entries(result, "lancamentos")).hasSize(1);
        money(result, "total", "500,00");
    }

    @Test
    void noMatchingEntriesRendersEmptyStateAndZeroTotal() throws Exception {
        var result = mvc.perform(get("/detalhes").with(user("kleber"))
                .param("periodo", "2026-11").param("responsavel", "Kleber").param("tipo", "GUARDADO"))
                .andExpect(status().isOk()).andReturn();
        assertThat(entries(viewModel(result), "lancamentos")).isEmpty();
        money(viewModel(result), "total", "0,00");
        assertThat(result.getResponse().getContentAsString()).contains("Nenhum lançamento encontrado neste período.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-13", "", "nao-e-periodo"})
    void malformedPeriodReturnsBadRequestInsteadOfServerError(String period) throws Exception {
        mvc.perform(get("/detalhes").with(user("kleber")).param("periodo", period)
                        .param("responsavel", "Kleber").param("tipo", "GUARDADO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingParametersAndUnknownTypeReturnBadRequest() throws Exception {
        mvc.perform(get("/detalhes").with(user("kleber"))).andExpect(status().isBadRequest());
        mvc.perform(get("/detalhes").with(user("kleber")).param("periodo", "2026-11")
                        .param("responsavel", "Kleber").param("tipo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }
}
