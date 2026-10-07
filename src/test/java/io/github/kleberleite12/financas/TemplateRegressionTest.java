package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TemplateRegressionTest extends WebIntegrationTest {
    Document html(String route, String actor) throws Exception {
        return Jsoup.parse(mvc.perform(get(route).with(user(actor)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void dashboardRendersRequestedScenarioAndBrazilianCurrency() throws Exception {
        scenarios();
        var html = html("/?ano=2026&mes=11", "kleber");
        assertThat(normalize(html.select(".painel-mes strong").text()))
                .contains("R$ 3.500,00", "R$ 1.250,00", "R$ 600,00", "R$ 1.650,00")
                .doesNotContain("R$ 3500.00", "R$ 600.00");
        assertThat(normalize(html.select(".pessoa").text())).contains("R$ 1.000,00", "R$ 20.000,00", "R$ 500,00");
        assertThat(html.select("a[href*=tipo=RENDA_PRINCIPAL]")).hasSize(2);
    }

    @Test
    void goalPercentagesUseBrazilianDecimalComma() throws Exception {
        scenarios();
        var html = html("/?ano=2026&mes=12", "giovanna");
        assertThat(html.select(".pessoa").text()).contains("5,0%", "2,5%");
    }

    @ParameterizedTest
    @CsvSource({"2026,1,2025,12,2026,2", "2026,12,2026,11,2027,1"})
    void monthNavigationLinksPreserveYearsAndMonths(int year, int month, int py, int pm, int ny, int nm) throws Exception {
        var html = html("/?ano=" + year + "&mes=" + month, "kleber");
        assertThat(html.select("a.botao-mes")).extracting(e -> e.attr("href"))
                .containsExactly("/?ano=" + py + "&mes=" + pm, "/?ano=" + ny + "&mes=" + nm);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?periodo=2026-11&resumo=receitas"})
    void listingFormatsIndividualAmountsAsBrazilianCurrency(String query) throws Exception {
        entry("Kleber", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "50.00");
        entry("Kleber", "2026-11-02", TipoLancamento.RENDA_EXTRA, "1000.00");
        var html = html("/lancamentos" + query, "kleber");
        assertThat(html.select("tbody tr td:nth-child(2)")).extracting(e -> normalize(e.text()))
                .containsExactly("R$ 1.000,00", "R$ 50,00");
        assertThat(html.select("a[href^=/lancamentos/editar/]")).hasSize(2);
        assertThat(html.select("form[action^=/lancamentos/excluir/] input[name=_csrf]")).hasSize(2);
    }

    @Test
    void detailsTemplateFormatsRowsAndTotalAndLinksBackToSelectedMonth() throws Exception {
        entry("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "600.00");
        entry("Kleber", "2026-11-02", TipoLancamento.GUARDADO, "3500.00");
        var html = html("/detalhes?periodo=2026-11&responsavel=Kleber&tipo=GUARDADO", "kleber");
        assertThat(html.select("tbody tr td:nth-child(3)")).extracting(e -> normalize(e.text()))
                .containsExactly("R$ 3.500,00", "R$ 600,00");
        assertThat(normalize(html.select(".destaques strong").text())).isEqualTo("R$ 4.100,00");
        assertThat(html.select("a.botao-principal").attr("href")).isEqualTo("/?ano=2026&mes=11");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "&periodo=2026-11&resumo=receitas"})
    void otherPersonsViewHasNoEditOrDeleteControls(String query) throws Exception {
        entry("Giovanna", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "50.00");
        var html = html("/lancamentos?visualizacao=outro" + query, "kleber");
        assertThat(html.select("tbody tr")).hasSize(1);
        assertThat(html.select("a[href^=/lancamentos/editar/],form[action^=/lancamentos/excluir/]")).isEmpty();
    }

    @Test
    void newFormKeepsClickMonthPickerCsrfAndOnlyActiveTypes() throws Exception {
        var html = html("/lancamentos/novo", "kleber");
        var month = html.selectFirst("input[name=periodoLancamento]");
        assertThat(month.attr("type")).isEqualTo("month");
        assertThat(month.attr("onclick")).isEqualTo("if (this.showPicker) this.showPicker();");
        assertThat(html.select("select[name=tipo] option")).extracting(e -> e.attr("value"))
                .containsExactly("", "RENDA_PRINCIPAL", "RENDA_EXTRA", "CARTAO_CREDITO", "OUTRO_GASTO", "GUARDADO");
        assertThat(html.select("form[action=/lancamentos] input[name=_csrf]")).hasSize(1);
    }

    @Test
    void descriptionsAndCategoriesAreHtmlEscaped() throws Exception {
        var entry = entry("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "50.00");
        entry.setDescricao("<script>alert('x')</script>");
        entry.setCategoria("<img src=x onerror=alert('x')>");
        repository.saveAndFlush(entry);
        var html = html("/lancamentos", "kleber");
        assertThat(html.select("tbody script,tbody img")).isEmpty();
        assertThat(html.select("tbody").text()).contains(entry.getDescricao(), entry.getCategoria());
    }

    @Test
    void loginErrorAndLogoutMessagesAreRendered() throws Exception {
        var error = html("/login?error", "kleber");
        assertThat(error.select(".mensagem.erro").text()).isEqualTo("Usuário ou senha incorretos.");
        var logout = html("/login?logout", "kleber");
        assertThat(logout.select(".mensagem.sucesso").text()).isEqualTo("Você saiu da sua conta.");
    }
}
