package rastreia.lti.web.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AutenticacaoService autenticacaoService;

    public UsuarioService(UsuarioRepository usuarioRepository, AutenticacaoService autenticacaoService) {
        this.usuarioRepository = usuarioRepository;
        this.autenticacaoService = autenticacaoService;
    }

    @Transactional
    public Usuario cadastrar(Usuario dados) {
        validarCamposObrigatorios(dados);

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dados.getNome().trim());
        novoUsuario.setSobrenome(dados.getSobrenome().trim());
        novoUsuario.setEmail(dados.getEmail().trim().toLowerCase(Locale.ROOT));
        novoUsuario.setCpf(dados.getCpf());
        novoUsuario.setSenha(dados.getSenha());
        novoUsuario.setTipo_usuario(dados.getTipo_usuario());

        validarFormatoDados(novoUsuario);

        String cpfNumeros = novoUsuario.getCpf().replace(".", "").replace("-", "");
        novoUsuario.setCpf(cpfNumeros);
        validarDuplicidade(novoUsuario.getEmail(), cpfNumeros);

        novoUsuario.setSenha(autenticacaoService.encodarSenha(novoUsuario.getSenha()));
        novoUsuario.setCriadoEm(LocalDateTime.now());

        return usuarioRepository.save(novoUsuario);
    }

    public void validarCamposObrigatorios(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Informe os dados do usuário.");
        }

        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new IllegalArgumentException("Informe o nome.");
        }

        if (usuario.getSobrenome() == null || usuario.getSobrenome().isBlank()) {
            throw new IllegalArgumentException("Informe o sobrenome.");
        }

        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            throw new IllegalArgumentException("Informe o CPF.");
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }

        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new IllegalArgumentException("Informe a senha.");
        }

        if (usuario.getTipo_usuario() == null) {
            throw new IllegalArgumentException("Selecione o tipo de usuário.");
        }
    }

    public void validarFormatoDados(Usuario usuario) {
        validarCamposObrigatorios(usuario);

        if (usuario.getNome().length() > 50 || usuario.getSobrenome().length() > 50) {
            throw new IllegalArgumentException("Nome e sobrenome devem ter no máximo 50 caracteres cada.");
        }

        if (usuario.getCpf().length() > 14) {
            throw new IllegalArgumentException("O CPF deve ter no máximo 14 caracteres.");
        }

        if (usuario.getEmail().length() > 100) {
            throw new IllegalArgumentException("O e-mail deve ter no máximo 100 caracteres.");
        }

        if (!usuario.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }

        validarCpf(usuario.getCpf());
        validarSenha(usuario.getSenha());
    }

    public void validarSenha(String senha) {
        if (!autenticacaoService.senhaValida(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ocupar no máximo 72 bytes em UTF-8.");
        }
    }

    public void validarCpf(String cpf) {
        if (cpf == null || !cpf.matches("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}")) {
            throw new IllegalArgumentException("Informe o CPF com 11 números, com ou sem pontuação.");
        }

        String numeros = cpf.replace(".", "").replace("-", "");

        if (numeros.matches("([0-9])\\1{10}")) {
            throw new IllegalArgumentException("Informe um CPF válido.");
        }

        for (int digito = 9; digito <= 10; digito++) {
            int soma = 0;

            for (int i = 0; i < digito; i++) {
                soma += (numeros.charAt(i) - '0') * (digito + 1 - i);
            }

            int verificador = (soma * 10) % 11;
            if (verificador == 10) {
                verificador = 0;
            }

            if (verificador != numeros.charAt(digito) - '0') {
                throw new IllegalArgumentException("Informe um CPF válido.");
            }
        }
    }

    public void validarDuplicidade(String email, String cpf) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        validarCpf(cpf);
        String numeros = cpf.replace(".", "").replace("-", "");
        String formatado = numeros.substring(0, 3) + "." + numeros.substring(3, 6) + "."
                + numeros.substring(6, 9) + "-" + numeros.substring(9);

        if (usuarioRepository.existsByCpf(numeros) || usuarioRepository.existsByCpf(formatado)) {
            throw new IllegalArgumentException("Este CPF já está cadastrado.");
        }
    }
}
