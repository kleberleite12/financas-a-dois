package io.github.kleberleite12.financas.support;

import io.github.kleberleite12.financas.model.Lancamento;
import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real security, MVC, Thymeleaf and JPA; one cached context, fresh in-memory data per test. */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
public abstract class WebIntegrationTest {
    @Autowired protected MockMvc mvc;
    @Autowired protected LancamentoRepository repository;
    @Autowired private DataSource dataSource;

    @DynamicPropertySource
    static void forceIsolatedTestBindings(DynamicPropertyRegistry properties) {
        // Higher priority than machine environment variables: no real DB or passwords can leak in.
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:financas_tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("app.kleber.password", () -> "kleber-test-only");
        properties.add("app.giovanna.password", () -> "giovanna-test-only");
    }

    @BeforeEach
    void isolateDatabase() throws Exception {
        // Fail closed before any delete: never clear a database outside the H2 test profile.
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"))
                    .as("Limpeza permitida somente no banco H2 em memória").isTrue();
        }
        repository.deleteAll();
    }

    protected Lancamento entry(String owner, String date, TipoLancamento type, String amount) {
        Lancamento entry = new Lancamento();
        entry.setResponsavel(owner);
        entry.setData(LocalDate.parse(date));
        entry.setTipo(type);
        entry.setValor(new BigDecimal(amount));
        entry.setDescricao("Registro " + type);
        entry.setCategoria("Categoria " + type);
        return repository.saveAndFlush(entry);
    }

    protected void scenarios() {
        entry("Kleber", "2026-11-01", TipoLancamento.RENDA_PRINCIPAL, "3000.00");
        entry("Kleber", "2026-11-15", TipoLancamento.RENDA_EXTRA, "500.00");
        entry("Kleber", "2026-11-30", TipoLancamento.CARTAO_CREDITO, "1050.00");
        entry("Kleber", "2026-11-02", TipoLancamento.OUTRO_GASTO, "200.00");
        entry("Kleber", "2026-11-03", TipoLancamento.GUARDADO, "600.00");
        entry("Kleber", "2026-12-01", TipoLancamento.GUARDADO, "400.00");
        entry("Giovanna", "2026-12-01", TipoLancamento.RENDA_PRINCIPAL, "2500.00");
        entry("Giovanna", "2026-12-02", TipoLancamento.RENDA_EXTRA, "200.00");
        entry("Giovanna", "2026-12-03", TipoLancamento.CARTAO_CREDITO, "300.00");
        entry("Giovanna", "2026-12-04", TipoLancamento.OUTRO_GASTO, "100.00");
        entry("Giovanna", "2026-12-05", TipoLancamento.GUARDADO, "500.00");
    }

    protected MockHttpServletRequestBuilder validPost(String username) {
        return formPost(username, Map.of());
    }

    protected MockHttpServletRequestBuilder formPost(String username, Map<String, String> overrides) {
        Map<String, String> fields = new LinkedHashMap<>(Map.of(
                "valor", "50.00", "tipo", "RENDA_EXTRA", "periodoLancamento", "2026-11",
                "descricao", "Descrição teste", "categoria", "Categoria teste"));
        fields.putAll(overrides);
        var request = post("/lancamentos").with(user(username)).with(csrf());
        fields.forEach((name, value) -> { if (value != null) request.param(name, value); });
        return request;
    }

    protected Map<String, Object> dashboard(String username, int year, int month) throws Exception {
        return mvc.perform(get("/").with(user(username))
                        .param("ano", String.valueOf(year)).param("mes", String.valueOf(month)))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
    }

    protected static Map<String, Object> viewModel(MvcResult result) {
        return result.getModelAndView().getModel();
    }

    @SuppressWarnings("unchecked")
    protected static List<Lancamento> entries(Map<String, Object> model, String attribute) {
        return (List<Lancamento>) model.get(attribute);
    }

    protected static void money(Map<String, Object> model, String attribute, String expected) {
        String formatted = expected.startsWith("-") ? "-R$ " + expected.substring(1) : "R$ " + expected;
        assertThat(normalize((String) model.get(attribute))).as(attribute).isEqualTo(formatted);
    }

    public static String normalize(String text) {
        return text.replace('\u00a0', ' ').replace('\u202f', ' ');
    }
}
