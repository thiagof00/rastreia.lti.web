package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;

class UsuarioServiceTests {

    // Estes testes verificam apenas regras locais, que não utilizam o Repository.
    private final UsuarioService service = new UsuarioService(null, new AutenticacaoService(null, null));

    @Test
    void aceitaSenhaValidaAteLimiteDoBCrypt() {
        assertDoesNotThrow(() -> service.validarSenha("Teste123"));
        assertDoesNotThrow(() -> service.validarSenha("A1" + "x".repeat(70)));
    }

    @Test
    void rejeitaSenhaSemRequisitosOuAcimaDoLimite() {
        for (String senha : new String[] {null, "", "Teste12", "abcdefgh", "12345678",
                "A1" + "x".repeat(71), "A1" + "á".repeat(36)}) {
            assertThrows(IllegalArgumentException.class, () -> service.validarSenha(senha));
        }
    }

    @Test
    void cadastroRejeitaSenhaInvalidaMesmoComOutrosCamposValidos() {
        Usuario usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setSobrenome("Silva");
        usuario.setEmail("ana@example.com");
        usuario.setCpf("52998224725");
        usuario.setSenha("123");
        usuario.setTipo_usuario(TipoUsuario.CLIENTE);

        assertThrows(IllegalArgumentException.class, () -> service.validarFormatoDados(usuario));
    }

    @Test
    void aceitaCpfComOuSemPontuacaoEVerificadorZero() {
        for (String cpf : new String[] {"52998224725", "529.982.247-25", "11144477735", "12345678909"}) {
            assertDoesNotThrow(() -> service.validarCpf(cpf), cpf);
        }
    }

    @Test
    void rejeitaCpfInvalido() {
        for (String cpf : new String[] {null, "", "00000000000", "111.111.111-11", "123",
                "52998224724", "52998224715", "5299822472a", "529.982247-25", "529982247250"}) {
            assertThrows(IllegalArgumentException.class, () -> service.validarCpf(cpf), cpf);
        }
    }

    @Test
    void validacaoDoCadastroTambemRejeitaCpfInvalido() {
        Usuario usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setSobrenome("Silva");
        usuario.setEmail("ana@example.com");
        usuario.setCpf("52998224724");
        usuario.setSenha("Teste1234");
        usuario.setTipo_usuario(TipoUsuario.CLIENTE);

        assertThrows(IllegalArgumentException.class, () -> service.validarFormatoDados(usuario));
    }
}
