package rastreia.lti.web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import rastreia.lti.web.models.Motorista;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    Page<Motorista> findAllByAtivo(boolean ativo, Pageable pageable);

}
