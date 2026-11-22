package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AutenticacaoServiceTests {

    @Test
    void geraHashQueCorrespondeASenhaValida() {
        AutenticacaoService service = new AutenticacaoService(null, null);
        String senha = "Teste123";

        String hash = service.encodarSenha(senha);

        assertTrue(new BCryptPasswordEncoder().matches(senha, hash));
    }

    @Test
    void rejeitaSenhaInvalidaAntesDeGerarHash() {
        AutenticacaoService service = new AutenticacaoService(null, null);

        assertThrows(IllegalArgumentException.class, () -> service.encodarSenha("123"));
    }
}
