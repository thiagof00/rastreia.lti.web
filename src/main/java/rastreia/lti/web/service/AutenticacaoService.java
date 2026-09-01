package rastreia.lti.web.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import rastreia.lti.web.models.Sessao;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.SessaoRepository;
import rastreia.lti.web.repository.UsuarioRepository;

@Service
public class AutenticacaoService {

    private static final long HORAS_EXPIRACAO_SESSAO = 8;
    private static final int MAX_TENTATIVAS = 5;
    private static final long MINUTOS_BLOQUEIO = 15;
    private static final String REGEX_SENHA = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private final UsuarioRepository usuarioRepository;
    private final SessaoRepository sessaoRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AutenticacaoService(UsuarioRepository usuarioRepository, SessaoRepository sessaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoRepository = sessaoRepository;
    }

    public boolean estaBloqueado(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        return usuario != null
                && usuario.getBloqueadoAte() != null
                && LocalDateTime.now().isBefore(usuario.getBloqueadoAte());
    }

    public String login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null || !usuario.getAtivo()) {
            return null;
        }

        if (estaBloqueado(email)) {
            return null;
        }

        if (!encoder.matches(senha, usuario.getSenha())) {
            registrarTentativaFalha(usuario);
            return null;
        }

        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);

        Sessao sessao = new Sessao();
        sessao.setToken(UUID.randomUUID().toString());
        sessao.setUsuario(usuario);
        sessao.setExpiraEm(LocalDateTime.now().plusHours(HORAS_EXPIRACAO_SESSAO));
        sessaoRepository.save(sessao);

        return sessao.getToken();
    }

    private void registrarTentativaFalha(Usuario usuario) {
        int tentativas = usuario.getTentativasFalhas() + 1;
        usuario.setTentativasFalhas(tentativas);

        if (tentativas >= MAX_TENTATIVAS) {
            usuario.setBloqueadoAte(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEIO));
        }

        usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario validar(String token) {
        if (token == null) {
            return null;
        }

        Sessao sessao = sessaoRepository.findByToken(token);

        if (sessao == null) {
            return null;
        }

        if (sessao.expirada()) {
            sessaoRepository.deleteByToken(token);
            return null;
        }

        return sessao.getUsuario();
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            sessaoRepository.deleteByToken(token);
        }
    }

    public boolean senhaValida(String senha) {
        return senha != null && senha.matches(REGEX_SENHA);
    }

    public String encodarSenha(String senha) {
        if (!senhaValida(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }

        return encoder.encode(senha);
    }
}
