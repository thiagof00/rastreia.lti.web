package rastreia.lti.web.service;

import org.springframework.stereotype.Service;

import rastreia.lti.web.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
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
