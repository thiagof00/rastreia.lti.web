package rastreia.lti.web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import rastreia.lti.web.models.Caminhao;

public interface CaminhaoRepository extends JpaRepository<Caminhao, Long> {

    Page<Caminhao> findAllByAtivo(boolean ativo, Pageable pageable);

    Long countByAtivo(boolean ativo);
}
