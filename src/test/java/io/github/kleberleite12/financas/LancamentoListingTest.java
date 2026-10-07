package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.controller.LancamentoController.GrupoLancamentos;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LancamentoListingTest extends WebIntegrationTest {
    @SuppressWarnings("unchecked")
    List<GrupoLancamentos> groups(Map<String, Object> model) {
        return (List<GrupoLancamentos>) model.get("gruposLancamentos");
    }

    @ParameterizedTest
    @CsvSource({"kleber,Kleber,Giovanna", "giovanna,Giovanna,Kleber"})
    void defaultListingAndOtherPersonViewAreSeparated(String actor, String owner, String other) throws Exception {
        scenarios();
        var own = viewModel(mvc.perform(get("/lancamentos").with(user(actor)))
                .andExpect(status().isOk()).andReturn());
        assertThat(own).containsEntry("responsavelExibido", owner).containsEntry("visualizandoOutro", false);
        assertThat(groups(own)).isNotEmpty();
        assertThat(groups(own).stream().flatMap(g -> g.getLancamentos().stream()))
                .allMatch(e -> e.getResponsavel().equals(owner));
        var others = viewModel(mvc.perform(get("/lancamentos").with(user(actor)).param("visualizacao", "outro"))
                .andExpect(status().isOk()).andReturn());
        assertThat(others).containsEntry("responsavelExibido", other).containsEntry("visualizandoOutro", true);
        assertThat(groups(others)).isNotEmpty();
        assertThat(groups(others).stream().flatMap(g -> g.getLancamentos().stream()))
                .allMatch(e -> e.getResponsavel().equals(other));
    }

    @Test
    void monthGroupsAreDescendingWithIndependentSummariesAndNoLeakage() throws Exception {
        scenarios(); entry("Kleber", "2026-10-20", TipoLancamento.RENDA_EXTRA, "50.00");
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))).andReturn());
        var groups = groups(result);
        assertThat(groups).extracting(GrupoLancamentos::getPeriodo).containsExactly("2026-12", "2026-11", "2026-10");
        assertThat(normalize(groups.get(0).getGuardado())).isEqualTo("R$ 400,00");
        assertThat(normalize(groups.get(1).getReceitas())).isEqualTo("R$ 3.500,00");
        assertThat(normalize(groups.get(1).getGastos())).isEqualTo("R$ 1.250,00");
        assertThat(normalize(groups.get(1).getGuardado())).isEqualTo("R$ 600,00");
        assertThat(normalize(groups.get(2).getReceitas())).isEqualTo("R$ 50,00");
        assertThat(normalize(groups.get(2).getGastos())).isEqualTo("R$ 0,00");
        for (var group : groups) {
            assertThat(group.getLancamentos()).allMatch(e -> e.getData().toString().startsWith(group.getPeriodo()));
        }
    }

    @Test
    void entriesAreSortedByDateThenDescendingId() throws Exception {
        var first = entry("Kleber", "2026-11-01", TipoLancamento.RENDA_EXTRA, "10");
        var second = entry("Kleber", "2026-11-01", TipoLancamento.RENDA_EXTRA, "20");
        var lastDay = entry("Kleber", "2026-11-30", TipoLancamento.RENDA_EXTRA, "30");
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))).andReturn());
        assertThat(groups(result).getFirst().getLancamentos()).extracting(e -> e.getId())
                .containsExactly(lastDay.getId(), second.getId(), first.getId());
    }

    @ParameterizedTest
    @CsvSource({"receitas,'3.500,00',2", "gastos,'1.250,00',2", "guardado,'600,00',1"})
    void quickFiltersReturnOnlySelectedMonthCategoryAndOwner(String summary, String total, int count) throws Exception {
        scenarios();
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))
                        .param("periodo", "2026-11").param("resumo", summary))
                .andExpect(status().isOk()).andReturn());
        assertThat(result).containsEntry("filtroAtivo", true).containsEntry("periodoFiltroFormatado", "Novembro de 2026");
        var entries = entries(result, "lancamentosFiltrados");
        assertThat(entries).hasSize(count).allMatch(e -> e.getResponsavel().equals("Kleber"))
                .allMatch(e -> e.getData().getMonthValue() == 11);
        var expectedTypes = switch (summary) {
            case "receitas" -> List.of(TipoLancamento.RENDA_PRINCIPAL, TipoLancamento.RENDA_EXTRA);
            case "gastos" -> List.of(TipoLancamento.CARTAO_CREDITO, TipoLancamento.OUTRO_GASTO);
            default -> List.of(TipoLancamento.GUARDADO);
        };
        assertThat(entries).allMatch(e -> expectedTypes.contains(e.getTipo()));
        money(result, "totalFiltro", total);
        var sum = entries.stream().map(e -> e.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total.replace(".", "").replace(",", "."));
    }

    @ParameterizedTest
    @CsvSource({"receitas,RECEITA,RENDA_PRINCIPAL", "gastos,GASTO,CARTAO_CREDITO"})
    void legacyTypesRemainInQuickFiltersAndMonthlyTotals(String summary, String legacy, String active) throws Exception {
        entry("Kleber", "2026-11-01", TipoLancamento.valueOf(legacy), "100.00");
        entry("Kleber", "2026-11-02", TipoLancamento.valueOf(active), "50.00");
        entry("Giovanna", "2026-11-01", TipoLancamento.valueOf(legacy), "999.00");
        entry("Kleber", "2026-12-01", TipoLancamento.valueOf(legacy), "999.00");
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))
                .param("periodo", "2026-11").param("resumo", summary)).andReturn());
        assertThat(entries(result, "lancamentosFiltrados")).hasSize(2)
                .extracting(e -> e.getTipo()).containsExactly(TipoLancamento.valueOf(active), TipoLancamento.valueOf(legacy));
        money(result, "totalFiltro", "150,00");
        var november = groups(result).stream().filter(g -> g.getPeriodo().equals("2026-11")).findFirst().orElseThrow();
        assertThat(normalize(summary.equals("receitas") ? november.getReceitas() : november.getGastos()))
                .isEqualTo("R$ 150,00");
    }

    @Test
    void otherPersonsQuickFilterUsesOtherPersonsData() throws Exception {
        scenarios();
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))
                .param("visualizacao", "outro").param("periodo", "2026-12").param("resumo", "receitas")).andReturn());
        assertThat(entries(result, "lancamentosFiltrados")).hasSize(2)
                .allMatch(e -> e.getResponsavel().equals("Giovanna"));
        money(result, "totalFiltro", "2.700,00");
    }

    @ParameterizedTest
    @CsvSource({"2026-13,receitas", "2026-11,inexistente", "'',receitas", "2026-11,''"})
    void invalidFilterFallsBackToNormalList(String period, String summary) throws Exception {
        scenarios();
        mvc.perform(get("/lancamentos").with(user("kleber")).param("periodo", period).param("resumo", summary))
                .andExpect(status().isOk()).andExpect(model().attribute("filtroAtivo", false));
    }

    @Test
    void emptyFilteredMonthHasZeroTotal() throws Exception {
        scenarios();
        var result = viewModel(mvc.perform(get("/lancamentos").with(user("kleber"))
                .param("periodo", "2027-01").param("resumo", "receitas")).andReturn());
        assertThat(entries(result, "lancamentosFiltrados")).isEmpty();
        money(result, "totalFiltro", "0,00");
    }
}
