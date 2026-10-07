package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HomeIntegrationTest extends WebIntegrationTest {
    @ParameterizedTest
    @CsvSource({"kleber,11,'3.500,00','1.250,00','600,00','1.650,00'",
            "kleber,12,'0,00','0,00','400,00','-400,00'",
            "giovanna,12,'2.700,00','400,00','500,00','1.800,00'"})
    void requestedFinancialScenariosWorkThroughHttpAndJpa(String actor, int month, String income,
                                                         String expenses, String saved, String available) throws Exception {
        scenarios();
        var result = dashboard(actor, 2026, month);
        money(result, "minhasReceitas", income);
        money(result, "meusGastos", expenses);
        money(result, "meuGuardadoMes", saved);
        money(result, "meuDisponivelAgora", available);
        money(result, "kleberGuardadoAcumulado", "1.000,00");
        money(result, "giovannaGuardadoAcumulado", "500,00");
        assertThat(result.get("kleberPercentualMeta")).isEqualTo(new BigDecimal("5.0"));
        assertThat(result.get("giovannaPercentualMeta")).isEqualTo(new BigDecimal("2.5"));
    }
}
