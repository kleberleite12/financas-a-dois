package io.github.kleberleite12.financas;

import io.github.kleberleite12.financas.model.TipoLancamento;
import io.github.kleberleite12.financas.support.WebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LancamentoSecurityTest extends WebIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"/", "/lancamentos", "/lancamentos/novo", "/detalhes",
            "/lancamentos/editar/1"})
    void anonymousCannotReadProtectedRoutes(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/lancamentos", "/lancamentos/excluir/1"})
    void anonymousCannotWriteEvenWithCsrf(String route) throws Exception {
        mvc.perform(post(route).with(csrf())).andExpect(status().isFound())
                .andExpect(header().string("Location", endsWith("/login")));
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest(name = "Login válido: {0}")
    @ValueSource(strings = {"kleber", "giovanna"})
    void realLoginRedirectsToDashboard(String username) throws Exception {
        var result = mvc.perform(formLogin().user(username).password(username + "-test-only"))
                .andExpect(authenticated().withUsername(username))
                .andExpect(redirectedUrl("/")).andReturn();
        mvc.perform(get("/").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(view().name("home"))
                .andExpect(model().attribute("nomeUsuario", username.equals("kleber") ? "Kleber" : "Giovanna"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"kleber", "giovanna", "usuario-inexistente"})
    void invalidLoginFails(String username) throws Exception {
        mvc.perform(formLogin().user(username).password("wrong-test-only"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void successfulLoginOverridesPreviouslySavedRequest() throws Exception {
        var saved = mvc.perform(get("/lancamentos")).andReturn();
        mvc.perform(post("/login").session((MockHttpSession) saved.getRequest().getSession())
                        .with(csrf()).param("username", "kleber").param("password", "kleber-test-only"))
                .andExpect(authenticated()).andExpect(redirectedUrl("/"));
    }

    @Test
    void logoutInvalidatesRealLoginSession() throws Exception {
        var login = mvc.perform(formLogin().user("kleber").password("kleber-test-only")).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/")).andExpect(status().isFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/lancamentos", "/lancamentos/excluir/1", "/logout"})
    void csrfIsRequiredForPost(String route) throws Exception {
        var entry = entry("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "50.00");
        mvc.perform(post(route).with(user("kleber"))).andExpect(status().isForbidden());
        assertThat(repository.existsById(entry.getId())).isTrue();
    }

    @Test
    void invalidCsrfDoesNotCreateEntry() throws Exception {
        mvc.perform(post("/lancamentos").with(user("kleber")).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"kleber,Giovanna", "giovanna,Kleber"})
    void otherPersonsIdsCannotBeUsedToReadEditForm(String actor, String owner) throws Exception {
        var entry = entry(owner, "2026-11-01", TipoLancamento.RENDA_EXTRA, "50.00");
        mvc.perform(get("/lancamentos/editar/{id}", entry.getId()).with(user(actor)))
                .andExpect(status().isForbidden());
        assertThat(repository.findById(entry.getId()).orElseThrow().getResponsavel()).isEqualTo(owner);
    }

    @ParameterizedTest
    @CsvSource({"kleber,Giovanna", "giovanna,Kleber"})
    void forgedIdCannotModifyOtherPersonsEntry(String actor, String owner) throws Exception {
        var entry = entry(owner, "2026-11-01", TipoLancamento.RENDA_EXTRA, "50.00");
        mvc.perform(validPost(actor).param("id", entry.getId().toString()).param("responsavel", actor))
                .andExpect(status().isForbidden());
        var unchanged = repository.findById(entry.getId()).orElseThrow();
        assertThat(unchanged.getResponsavel()).isEqualTo(owner);
        assertThat(unchanged.getValor()).isEqualByComparingTo("50.00");
        assertThat(unchanged.getDescricao()).isEqualTo(entry.getDescricao());
        assertThat(unchanged.getCategoria()).isEqualTo(entry.getCategoria());
        assertThat(unchanged.getData()).isEqualTo(entry.getData());
        assertThat(unchanged.getTipo()).isEqualTo(entry.getTipo());
        assertThat(repository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"kleber,Giovanna", "giovanna,Kleber"})
    void forgedIdCannotDeleteOtherPersonsEntry(String actor, String owner) throws Exception {
        var entry = entry(owner, "2026-11-01", TipoLancamento.RENDA_EXTRA, "50.00");
        mvc.perform(post("/lancamentos/excluir/{id}", entry.getId()).with(user(actor)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(repository.existsById(entry.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"kleber", "giovanna"})
    void viewingOtherPersonDoesNotGrantWritePermission(String actor) throws Exception {
        var owner = actor.equals("kleber") ? "Giovanna" : "Kleber";
        var entry = entry(owner, "2026-11-01", TipoLancamento.GUARDADO, "50.00");
        mvc.perform(get("/lancamentos").with(user(actor)).param("visualizacao", "outro"))
                .andExpect(status().isOk()).andExpect(model().attribute("responsavelExibido", owner));
        mvc.perform(validPost(actor).param("id", entry.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/lancamentos/excluir/{id}", entry.getId()).with(user(actor)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(repository.existsById(entry.getId())).isTrue();
    }

    @Test
    void deletionCannotUseGet() throws Exception {
        var entry = entry("Kleber", "2026-11-01", TipoLancamento.GUARDADO, "50.00");
        mvc.perform(get("/lancamentos/excluir/{id}", entry.getId()).with(user("kleber")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(repository.existsById(entry.getId())).isTrue();
    }

    @Test
    void loginAndCssArePublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
    }
}
