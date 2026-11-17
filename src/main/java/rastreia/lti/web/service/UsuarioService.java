package rastreia.lti.web.service;

import org.springframework.stereotype.Service;

import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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

    public void validarDuplicidade(String email, String cpf) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        if (usuarioRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("Este CPF já está cadastrado.");
        }
    }
}
