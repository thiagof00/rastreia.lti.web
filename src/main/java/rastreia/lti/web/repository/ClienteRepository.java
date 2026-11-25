package rastreia.lti.web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import rastreia.lti.web.models.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("""
            select c from Cliente c
            where locate(lower(:busca), lower(c.razao_social)) > 0
               or locate(lower(:busca), lower(c.nome_fantasia)) > 0
               or locate(:busca, c.cpf) > 0
               or locate(:busca, c.cnpj) > 0
            """)
    Page<Cliente> buscar(@Param("busca") String busca, Pageable pageable);
}
