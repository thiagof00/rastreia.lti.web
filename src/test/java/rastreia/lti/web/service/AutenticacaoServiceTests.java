package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import rastreia.lti.web.models.Sessao;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.SessaoRepository;

class AutenticacaoServiceTests {

    // Confirma que a inativação da conta bloqueia o acesso com uma sessão ainda vigente.
    @Test
    void recusaSessaoVigenteDeUsuarioInativo() {
        String token = "token-de-teste";
        Usuario usuario = new Usuario();
        Sessao sessao = new Sessao();
        sessao.setToken(token);
        sessao.setUsuario(usuario);
        sessao.setExpiraEm(LocalDateTime.now().plusHours(1));

        SessaoRepository repositorio = (SessaoRepository) Proxy.newProxyInstance(
                SessaoRepository.class.getClassLoader(), new Class<?>[] {SessaoRepository.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "findByToken" -> token.equals(argumentos[0]) ? sessao : null;
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
        AutenticacaoService service = new AutenticacaoService(null, repositorio);

        assertEquals(usuario, service.validar(token));

        usuario.setAtivo(false);

        assertNull(service.validar(token));
    }

    // Confirma que o serviço recusa uma sessão expirada e solicita a remoção do token.
    @Test
    void recusaSessaoExpiradaESolicitaRemocao() {
        String token = "token-de-teste";
        Sessao sessao = new Sessao();
        sessao.setToken(token);
        sessao.setUsuario(new Usuario());
        sessao.setExpiraEm(LocalDateTime.now().minusMinutes(5));

        List<String> tokensRemovidos = new ArrayList<>();
        SessaoRepository repositorio = (SessaoRepository) Proxy.newProxyInstance(
                SessaoRepository.class.getClassLoader(), new Class<?>[] {SessaoRepository.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "findByToken" -> token.equals(argumentos[0]) ? sessao : null;
                    case "deleteByToken" -> {
                        tokensRemovidos.add((String) argumentos[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
        AutenticacaoService service = new AutenticacaoService(null, repositorio);

        assertNull(service.validar(token));
        assertEquals(List.of(token), tokensRemovidos);
    }

    // Confirma que o BCrypt reconhece a senha original ao verificar o hash gerado.
    @Test
    void geraHashQueCorrespondeASenhaValida() {
        AutenticacaoService service = new AutenticacaoService(null, null);
        String senha = "Teste123";

        String hash = service.encodarSenha(senha);

        assertTrue(new BCryptPasswordEncoder().matches(senha, hash));
    }

    // Confirma que uma senha inválida provoca uma exceção antes da geração do hash.
    @Test
    void rejeitaSenhaInvalidaAntesDeGerarHash() {
        AutenticacaoService service = new AutenticacaoService(null, null);

        assertThrows(IllegalArgumentException.class, () -> service.encodarSenha("123"));
    }
}
