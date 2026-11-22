package rastreia.lti.web.controllers;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.UsuarioRepository;
import rastreia.lti.web.service.UsuarioService;

@Controller
public class UsuariosController {

    private static final String TOKEN_CADASTRO = "tokenCadastroUsuario";
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuariosController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/usuarios")
    public ModelAndView index(@RequestAttribute(name = "usuarioLogado", required = false) Usuario usuarioLogado) {
        validarAdministrador(usuarioLogado);
        ModelAndView mv = new ModelAndView("usuarios/index");
        mv.addObject("paginaAtiva", "usuarios");
        mv.addObject("tiposUsuario", TipoUsuario.values());
        mv.addObject("usuarios", usuarioRepository.findAllByAtivo(true));
        mv.addObject("totalUsuarios", usuarioRepository.countByAtivo(true));
        return mv;

    }

    @GetMapping("/novo-usuario")
    public ModelAndView novoUsuario(
            @RequestAttribute(name = "usuarioLogado", required = false) Usuario usuarioLogado,
            HttpSession sessao) {
        validarAdministrador(usuarioLogado);
        return formulario(new Usuario(), sessao);
    }

    @PostMapping("/novo-usuario")
    public ModelAndView cadastrar(
            @RequestAttribute(name = "usuarioLogado", required = false) Usuario usuarioLogado,
            @ModelAttribute("usuarioForm") Usuario dados, BindingResult erros,
            @RequestParam(name = "tokenCadastro", required = false) String tokenCadastro,
            HttpSession sessao, RedirectAttributes redirect) {
        validarAdministrador(usuarioLogado);

        if (tokenCadastro == null || !tokenCadastro.equals(sessao.getAttribute(TOKEN_CADASTRO))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Abra novamente o formulário de cadastro.");
        }

        String mensagem;
        if (erros.hasErrors()) {
            mensagem = "Confira os dados informados no formulário.";
        } else {
            try {
                usuarioService.cadastrar(dados);
                redirect.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso.");
                return new ModelAndView("redirect:/novo-usuario");
            } catch (IllegalArgumentException e) {
                mensagem = e.getMessage();
            } catch (DataIntegrityViolationException e) {
                mensagem = "O banco recusou o cadastro. Confira se o CPF ou e-mail já está em uso.";
            }
        }

        dados.setSenha(null);
        ModelAndView mv = formulario(dados, sessao);
        mv.addObject("erro", mensagem);
        return mv;
    }

    private ModelAndView formulario(Usuario usuario, HttpSession sessao) {
        String token = (String) sessao.getAttribute(TOKEN_CADASTRO);
        if (token == null) {
            token = UUID.randomUUID().toString();
            sessao.setAttribute(TOKEN_CADASTRO, token);
        }

        ModelAndView mv = new ModelAndView("usuarios/formNewUsuario");
        mv.addObject("paginaAtiva", "usuarios");
        mv.addObject("usuarioForm", usuario);
        mv.addObject("tokenCadastro", token);
        mv.addObject("tiposUsuario", TipoUsuario.values());
        return mv;
    }

    private void validarAdministrador(Usuario usuarioLogado) {
        if (usuarioLogado == null || !Boolean.TRUE.equals(usuarioLogado.getAtivo())
                || usuarioLogado.getTipo_usuario() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o administrador pode cadastrar usuários.");
        }
    }
}
