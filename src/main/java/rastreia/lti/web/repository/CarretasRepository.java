package rastreia.lti.web.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import rastreia.lti.web.models.Carreta;

public interface CarretasRepository extends JpaRepository<Carreta, Long> {

    Page<Carreta> findAllByAtivo(boolean ativo, Pageable pageable);

}
