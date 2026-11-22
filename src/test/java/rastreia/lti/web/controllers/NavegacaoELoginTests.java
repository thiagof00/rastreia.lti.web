package rastreia.lti.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import jakarta.servlet.http.Cookie;
import rastreia.lti.web.interceptor.AutenticacaoInterceptor;
import rastreia.lti.web.service.AutenticacaoService;
import rastreia.lti.web.service.LimitadorDeRequisicoes;

class NavegacaoELoginTests {

    // Confirma que as rotas de leitura encaminham para as páginas esperadas.
    @Test
    void abreTelasDeNavegacao() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new GeralController(null, null),
                new CargaController(), new EtapasController())
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();

        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("login"));
        mvc.perform(get("/dashboard")).andExpect(status().isOk()).andExpect(view().name("dashboard"));
        mvc.perform(get("/cargas")).andExpect(status().isOk()).andExpect(view().name("cargas/index"));
        mvc.perform(get("/etapas")).andExpect(status().isOk()).andExpect(view().name("etapas/index"));
    }

    // Confirma que o login web entrega o cookie e que o logout solicita a remoção da sessão.
    @Test
    void entraESaiPelaWeb() throws Exception {
        var autenticacao = new AutenticacaoSimulada();
        var mvc = MockMvcBuilders.standaloneSetup(
                new GeralController(autenticacao, new LimitadorDeRequisicoes())).build();
        String nomeCookie = AutenticacaoInterceptor.NOME_COOKIE;

        mvc.perform(post("/login").param("usuario", "teste@example.test").param("password", "Teste123"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(cookie().value(nomeCookie, "sessao-de-teste"));
        mvc.perform(get("/logout").cookie(new Cookie(nomeCookie, "sessao-de-teste")))
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().maxAge(nomeCookie, 0));
        assertEquals("sessao-de-teste", autenticacao.tokenEncerrado);
    }

    // Confirma que a API recusa credenciais inválidas, entrega o token e encerra a sessão.
    @Test
    void entraESaiPelaApi() throws Exception {
        var autenticacao = new AutenticacaoSimulada();
        var mvc = MockMvcBuilders.standaloneSetup(
                new RestAppController(autenticacao, new LimitadorDeRequisicoes())).build();

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"teste@example.test\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"teste@example.test\",\"senha\":\"Teste123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("sessao-de-teste"));
        mvc.perform(post("/api/logout").header("Authorization", "Bearer sessao-de-teste"))
                .andExpect(status().isNoContent());
        assertEquals("sessao-de-teste", autenticacao.tokenEncerrado);
    }

    private static class AutenticacaoSimulada extends AutenticacaoService {
        private String tokenEncerrado;

        private AutenticacaoSimulada() {
            super(null, null);
        }

        @Override
        public boolean estaBloqueado(String email) {
            return false;
        }

        @Override
        public String login(String email, String senha) {
            return "Teste123".equals(senha) ? "sessao-de-teste" : null;
        }

        @Override
        public void logout(String token) {
            tokenEncerrado = token;
        }
    }
}
