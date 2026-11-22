package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class FalhaSenhaDemo {

    @Test
    void esperaAceitarSenhaInvalidaDeProposito() {
        AutenticacaoService service = new AutenticacaoService(null, null);

        // Expectativa propositalmente errada para demonstrar uma falha no JUnit.
        assertDoesNotThrow(() -> service.encodarSenha("123"));
    }
}
