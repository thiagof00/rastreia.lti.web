package rastreia.lti.web.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.UsuarioRepository;

class CadastroUsuarioTests {

    @Test
    void salvaNovaContaComSenhaProtegidaSemAlterarContaExistente() {
        AtomicReference<Usuario> salvo = new AtomicReference<>();
        UsuarioService service = service(salvo, null, null);
        Usuario dados = dadosValidos();
        dados.setId(99L);
        dados.setAtivo(false);
        dados.setTentativasFalhas(5);

        Usuario resultado = service.cadastrar(dados);

        assertSame(salvo.get(), resultado);
        assertNull(resultado.getId());
        assertEquals("Ana", resultado.getNome());
        assertEquals("ana@example.com", resultado.getEmail());
        assertEquals("52998224725", resultado.getCpf());
        assertTrue(resultado.getAtivo());
        assertEquals(0, resultado.getTentativasFalhas());
        assertNotNull(resultado.getCriadoEm());
        assertNotEquals(dados.getSenha(), resultado.getSenha());
        assertTrue(new BCryptPasswordEncoder().matches("Teste1234", resultado.getSenha()));
        assertEquals("Teste1234", dados.getSenha());
    }

    @Test
    void naoSalvaEmailRepetido() {
        AtomicReference<Usuario> salvo = new AtomicReference<>();
        UsuarioService service = service(salvo, "ANA@example.com", null);
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(dadosValidos()));
        assertNull(salvo.get());
    }

    @Test
    void naoSalvaCpfRepetidoComOuSemPontuacao() {
        for (String cpf : new String[] {"52998224725", "529.982.247-25"}) {
            AtomicReference<Usuario> salvo = new AtomicReference<>();
            UsuarioService service = service(salvo, null, cpf);
            assertThrows(IllegalArgumentException.class, () -> service.cadastrar(dadosValidos()));
            assertNull(salvo.get());
        }
    }

    @Test
    void naoSalvaDadosInvalidos() {
        AtomicReference<Usuario> salvo = new AtomicReference<>();
        UsuarioService service = service(salvo, null, null);
        Usuario dados = dadosValidos();
        dados.setSenha("123");
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(dados));
        assertNull(salvo.get());
    }

    private Usuario dadosValidos() {
        Usuario dados = new Usuario();
        dados.setNome(" Ana ");
        dados.setSobrenome(" Silva ");
        dados.setEmail(" ANA@example.com ");
        dados.setCpf("529.982.247-25");
        dados.setSenha("Teste1234");
        dados.setTipo_usuario(TipoUsuario.CLIENTE);
        return dados;
    }

    // Repository simulado em memória: nenhum destes testes conecta ao banco.
    private UsuarioService service(AtomicReference<Usuario> salvo, String emailExistente, String cpfExistente) {
        UsuarioRepository repository = (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(), new Class<?>[] {UsuarioRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmailIgnoreCase" -> emailExistente != null
                            && emailExistente.equalsIgnoreCase((String) args[0]);
                    case "existsByCpf" -> cpfExistente != null && cpfExistente.equals(args[0]);
                    case "save" -> {
                        salvo.set((Usuario) args[0]);
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new UsuarioService(repository, new AutenticacaoService(repository, null));
    }
}
