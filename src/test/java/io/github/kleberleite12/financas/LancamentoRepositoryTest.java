package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LancamentoRepositoryTest extends WebIntegrationTest {
    @Test
    void dateBetweenIncludesBothBoundariesAndExcludesOtherMonths() {
        entry("Kleber", "2026-10-31", TipoLancamento.GUARDADO, "1");
        var first = entry("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "2");
        var last = entry("Giovanna", "2026-11-30", TipoLancamento.GUARDADO, "3");
        entry("Kleber", "2026-12-01", TipoLancamento.GUARDADO, "4");
        assertThat(repository.findByDataBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .extracting(e -> e.getId()).containsExactlyInAnyOrder(first.getId(), last.getId());
    }

    @ParameterizedTest
    @EnumSource(TipoLancamento.class)
    void allActiveAndLegacyEnumsAndDecimalValuesRoundTrip(TipoLancamento type) {
        var entry = entry("Kleber", "2026-11-01", type, "1234.56");
        var loaded = repository.findById(entry.getId()).orElseThrow();
        assertThat(loaded.getTipo()).isEqualTo(type);
        assertThat(loaded.getValor()).isEqualByComparingTo("1234.56");
        assertThat(loaded.getData()).isEqualTo(entry.getData());
        assertThat(loaded.getResponsavel()).isEqualTo("Kleber");
    }
}
