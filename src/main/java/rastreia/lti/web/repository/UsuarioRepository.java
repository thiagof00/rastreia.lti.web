package rastreia.lti.web.repository;

import org.springframework.data.repository.CrudRepository;

import rastreia.lti.web.models.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    Usuario findByEmail(String email);

}
