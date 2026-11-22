package rastreia.lti.web.repository;

import org.springframework.data.repository.CrudRepository;

import rastreia.lti.web.models.Carreta;

public interface CarretasRepository extends CrudRepository<Carreta, Long> {
    Iterable<Carreta> findAllByAtivo(boolean ativo);

}
