package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class FalhaSenhaDemo {

    // Falha de propósito ao esperar que uma senha inválida seja aceita.
    @Test
    void esperaAceitarSenhaInvalidaDeProposito() {
        AutenticacaoService service = new AutenticacaoService(null, null);

        assertDoesNotThrow(() -> service.encodarSenha("123"));
    }
}
