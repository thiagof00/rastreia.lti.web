package rastreia.lti.web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.models.TipoUsuario;
import java.util.Collection;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    Usuario findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);

    Page<Usuario> findAllByAtivo(boolean ativo, Pageable pageable);

    long countByAtivo(boolean ativo);

    @Query("select count(u) from Usuario u where u.ativo = true and u.tipo_usuario = :tipo")
    long contarAtivosPorTipo(@Param("tipo") TipoUsuario tipo);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByCpfInAndIdNot(Collection<String> cpfs, Long id);

    @Query("""
            select u from Usuario u
            where u.ativo = true and (
                locate(lower(:busca), lower(concat(coalesce(u.nome, ''), ' ', coalesce(u.sobrenome, '')))) > 0
                or locate(:busca, u.cpf) > 0
                or locate(lower(:busca), lower(u.email)) > 0
            )
            """)
    Page<Usuario> buscarAtivos(@Param("busca") String busca, Pageable pageable);

    Iterable<Usuario> findAllByClienteIdIsNull();

    Iterable<Usuario> findAllByClienteIdIsNullAndAtivo(boolean ativo);

}
