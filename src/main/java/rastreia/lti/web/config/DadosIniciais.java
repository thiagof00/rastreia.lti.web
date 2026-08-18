package rastreia.lti.web.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.UsuarioRepository;
import rastreia.lti.web.service.AutenticacaoService;

@Component
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final AutenticacaoService autenticacaoService;

    public DadosIniciais(UsuarioRepository usuarioRepository, AutenticacaoService autenticacaoService) {
        this.usuarioRepository = usuarioRepository;
        this.autenticacaoService = autenticacaoService;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("thiago@iffar") != null) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail("thiago@iffar");
        admin.setSenha(autenticacaoService.encodarSenha("admin123"));
        admin.setCpf("01234567891112");
        admin.setTipo_usuario(TipoUsuario.DEV);
        usuarioRepository.save(admin);
    }
}
