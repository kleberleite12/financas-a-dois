package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.controller.HomeController;
import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.kleberleite12.financas.support.WebIntegrationTest.normalize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Focused financial calculations without starting Spring or a database. */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {
    @Mock LancamentoRepository repository;
    HomeController controller;
    List<Lancamento> data;

    @BeforeEach
    void prepare() {
        controller = new HomeController(repository);
        data = new ArrayList<>();
        when(repository.findAll()).thenAnswer(invocation -> List.copyOf(data));
        when(repository.findByDataBetween(any(), any())).thenAnswer(invocation -> {
            LocalDate start = invocation.getArgument(0), end = invocation.getArgument(1);
            return data.stream().filter(e -> !e.getData().isBefore(start) && !e.getData().isAfter(end)).toList();
        });
    }

    void add(String owner, String date, TipoLancamento type, String amount) {
        var e = new Lancamento(); e.setResponsavel(owner); e.setData(LocalDate.parse(date));
        e.setTipo(type); e.setValor(new BigDecimal(amount)); data.add(e);
    }

    Map<String, Object> home(String actor, Integer year, Integer month) {
        var model = new ExtendedModelMap();
        var auth = new UsernamePasswordAuthenticationToken(actor, null);
        assertThat(controller.home(year, month, auth, model)).isEqualTo("home");
        return model;
    }

    void money(Map<String, Object> model, String attribute, String expected) {
        String formatted = expected.startsWith("-") ? "-R$ " + expected.substring(1) : "R$ " + expected;
        assertThat(normalize((String) model.get(attribute))).as(attribute).isEqualTo(formatted);
    }

    void scenario() {
        add("Kleber", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "3000");
        add("Kleber", "2026-11-15", TipoLancamento.RENDA_EXTRA, "500");
        add("Kleber", "2026-11-30", TipoLancamento.CARTAO_CREDITO, "1050");
        add("Kleber", "2026-11-02", TipoLancamento.OUTRO_GASTO, "200");
        add("Kleber", "2026-11-03", TipoLancamento.GUARDADO, "600");
        add("Kleber", "2026-12-01", TipoLancamento.GUARDADO, "400");
        add("Giovanna", "2026-12-01", TipoLancamento.RENDA_PRINCIPAL, "2500");
        add("Giovanna", "2026-12-02", TipoLancamento.RENDA_EXTRA, "200");
        add("Giovanna", "2026-12-03", TipoLancamento.CARTAO_CREDITO, "300");
        add("Giovanna", "2026-12-04", TipoLancamento.OUTRO_GASTO, "100");
        add("Giovanna", "2026-12-05", TipoLancamento.GUARDADO, "500");
    }

    @Test
    void kleberNovemberHasCorrectIncomeExpenseSavingsAndAvailable() {
        scenario(); var model = home("kleber", 2026, 11);
        money(model, "minhasReceitas", "3.500,00");
        money(model, "meusGastos", "1.250,00");
        money(model, "meuGuardadoMes", "600,00");
        money(model, "meuDisponivelAgora", "1.650,00");
        money(model, "kleberRendaPrincipal", "3.000,00");
        money(model, "kleberRendaExtra", "500,00");
        money(model, "kleberCartaoCredito", "1.050,00");
        money(model, "kleberOutrosGastos", "200,00");
        assertThat(model.get("disponivelNegativo")).isEqualTo(false);
    }

    @Test
    void accumulatedGoalIncludesAllMonthsAndNeverMixesPeople() {
        scenario(); var november = home("kleber", 2026, 11); var december = home("kleber", 2026, 12);
        money(november, "kleberGuardadoMes", "600,00");
        money(december, "kleberGuardadoMes", "400,00");
        for (var model : List.of(november, december)) {
            money(model, "kleberGuardadoAcumulado", "1.000,00");
            money(model, "giovannaGuardadoAcumulado", "500,00");
            money(model, "metaIndividual", "20.000,00");
            assertThat(model.get("kleberPercentualMeta")).isEqualTo(new BigDecimal("5.0"));
            assertThat(model.get("giovannaPercentualMeta")).isEqualTo(new BigDecimal("2.5"));
        }
    }

    @Test
    void giovannaDecemberShowsHerOwnMainPanel() {
        scenario(); var model = home("giovanna", 2026, 12);
        assertThat(model.get("nomeUsuario")).isEqualTo("Giovanna");
        money(model, "minhasReceitas", "2.700,00");
        money(model, "meusGastos", "400,00");
        money(model, "meuGuardadoMes", "500,00");
        money(model, "meuDisponivelAgora", "1.800,00");
    }

    @Test
    void legacyTypesAreSalaryAndCreditCardInDashboard() {
        add("kLeBeR", "2026-11-01", TipoLancamento.RECEITA, "100");
        add("Kleber", "2026-11-02", TipoLancamento.RENDA_PRINCIPAL, "50");
        add("Kleber", "2026-11-03", TipoLancamento.GASTO, "25");
        add("Kleber", "2026-11-04", TipoLancamento.CARTAO_CREDITO, "10");
        var model = home("kleber", 2026, 11);
        money(model, "kleberRendaPrincipal", "150,00");
        money(model, "kleberCartaoCredito", "35,00");
        money(model, "minhasReceitas", "150,00");
        money(model, "meusGastos", "35,00");
        money(model, "meuDisponivelAgora", "115,00");
    }

    @Test
    void emptyMonthDoesNotLoseSavingsFromPriorMonths() {
        add("Kleber", "2026-10-01", TipoLancamento.GUARDADO, "600");
        var model = home("kleber", 2026, 11);
        money(model, "meuGuardadoMes", "0,00");
        money(model, "minhasReceitas", "0,00");
        money(model, "meusGastos", "0,00");
        money(model, "meuDisponivelAgora", "0,00");
        money(model, "kleberGuardadoAcumulado", "600,00");
    }

    @Test
    void negativeAvailableBalanceIsFlagged() {
        add("Kleber", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "100");
        add("Kleber", "2026-11-01", TipoLancamento.OUTRO_GASTO, "150");
        add("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "25");
        var model = home("kleber", 2026, 11);
        money(model, "meuDisponivelAgora", "-75,00");
        assertThat(model.get("disponivelNegativo")).isEqualTo(true);
    }

    @Test
    void progressBarCapsAt100WithoutCappingActualGoalPercentage() {
        add("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "25000");
        var model = home("kleber", 2026, 11);
        assertThat(model.get("kleberPercentualMeta")).isEqualTo(new BigDecimal("125.0"));
        assertThat(model.get("kleberPercentualBarra")).isEqualTo(new BigDecimal("100.0"));
        assertThat(model.get("giovannaPercentualBarra")).isEqualTo(new BigDecimal("0.0"));
    }

    @Test
    void sumsDecimalAmountsExactly() {
        add("Kleber", "2026-11-01", TipoLancamento.RENDA_EXTRA, "0.10");
        add("Kleber", "2026-11-01", TipoLancamento.RENDA_EXTRA, "0.20");
        add("Kleber", "2026-11-01", TipoLancamento.OUTRO_GASTO, "0.10");
        var model = home("kleber", 2026, 11);
        money(model, "minhasReceitas", "0,30");
        money(model, "meuDisponivelAgora", "0,20");
    }

    @ParameterizedTest
    @CsvSource({"2026,1,2025,12,2026,2", "2026,12,2026,11,2027,1", "2026,11,2026,10,2026,12"})
    void monthNavigationCrossesYearBoundaries(int year, int month, int previousYear, int previousMonth,
                                             int nextYear, int nextMonth) {
        var model = home("kleber", year, month);
        assertThat(model).containsEntry("anoAnterior", previousYear).containsEntry("mesAnterior", previousMonth)
                .containsEntry("proximoAno", nextYear).containsEntry("proximoMes", nextMonth);
    }

    @Test
    void leapYearIncludesLastDayButNotNeighbouringMonths() {
        add("Kleber", "2024-01-31", TipoLancamento.RENDA_EXTRA, "100");
        add("Kleber", "2024-02-01", TipoLancamento.RENDA_EXTRA, "10");
        add("Kleber", "2024-02-29", TipoLancamento.RENDA_EXTRA, "20");
        add("Kleber", "2024-03-01", TipoLancamento.RENDA_EXTRA, "100");
        money(home("kleber", 2024, 2), "minhasReceitas", "30,00");
        verify(repository).findByDataBetween(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1})
    void invalidMonthKeepsExistingFallbackToCurrentMonth(int month) {
        var before = YearMonth.now(); var model = home("kleber", 2026, month); var after = YearMonth.now();
        assertThat(model.get("periodoAtual")).isIn(before.toString(), after.toString());
    }

    @Test
    void absentPeriodDefaultsToCurrentMonth() {
        var before = YearMonth.now(); var model = home("kleber", null, null); var after = YearMonth.now();
        assertThat(model.get("periodoAtual")).isIn(before.toString(), after.toString());
    }
}
