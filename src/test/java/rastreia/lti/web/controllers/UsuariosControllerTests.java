package rastreia.lti.web.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;

class UsuariosControllerTests {

    private final UsuariosController controller = new UsuariosController(null);

    @Test
    void postExigeAdministradorETokenDoFormulario() throws Exception {
        Usuario admin = new Usuario();
        admin.setTipo_usuario(TipoUsuario.ADMIN);
        Usuario cliente = new Usuario();
        cliente.setTipo_usuario(TipoUsuario.CLIENTE);
        MockHttpSession sessao = new MockHttpSession();
        String token = (String) controller.novoUsuario(admin, sessao).getModel().get("tokenCadastro");
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/novo-usuario").session(sessao).requestAttr("usuarioLogado", admin))
                .andExpect(status().isForbidden());
        mvc.perform(post("/novo-usuario").session(sessao).requestAttr("usuarioLogado", cliente)
                .param("tokenCadastro", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void renderizaFormularioComLayoutECamposVinculados() throws Exception {
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);

        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");

        Usuario admin = new Usuario();
        admin.setTipo_usuario(TipoUsuario.ADMIN);
        String html = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(views).build()
                .perform(get("/novo-usuario").requestAttr("usuarioLogado", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("action=\"/novo-usuario\""));
        assertTrue(html.contains("name=\"nome\""));
        assertTrue(html.contains("name=\"tipo_usuario\""));
        assertTrue(html.contains("value=\"ADMIN\""));
        assertTrue(html.contains("value=\"CLIENTE\""));
        assertTrue(html.contains("value=\"DEV\""));
        assertTrue(html.contains("type=\"password\" id=\"senha\" name=\"senha\""));
        assertFalse(html.matches("(?s).*<input[^>]*name=\"senha\"[^>]*value=.*"));
    }

    @Test
    void administradorAtivoRecebeFormularioNovoSemSenha() {
        Usuario admin = new Usuario();
        admin.setTipo_usuario(TipoUsuario.ADMIN);

        ModelAndView mv = controller.novoUsuario(admin, new MockHttpSession());

        assertEquals("usuarios/formNewUsuario", mv.getViewName());
        assertEquals("usuarios", mv.getModel().get("paginaAtiva"));
        Usuario formulario = (Usuario) mv.getModel().get("usuarioForm");
        assertNull(formulario.getId());
        assertNull(formulario.getSenha());
        assertArrayEquals(TipoUsuario.values(), (TipoUsuario[]) mv.getModel().get("tiposUsuario"));
    }

    @Test
    void outrosPerfisNaoRecebemFormulario() {
        for (TipoUsuario tipo : new TipoUsuario[] {TipoUsuario.CLIENTE, TipoUsuario.DEV, null}) {
            Usuario usuario = new Usuario();
            usuario.setTipo_usuario(tipo);
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> controller.novoUsuario(usuario, new MockHttpSession()));
            assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        }
    }

    @Test
    void administradorInativoOuUsuarioAusenteNaoRecebeFormulario() {
        Usuario admin = new Usuario();
        admin.setTipo_usuario(TipoUsuario.ADMIN);
        admin.setAtivo(false);
        assertThrows(ResponseStatusException.class, () -> controller.novoUsuario(admin, new MockHttpSession()));
        assertThrows(ResponseStatusException.class, () -> controller.novoUsuario(null, new MockHttpSession()));
    }
}
