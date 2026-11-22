package rastreia.lti.web.repository;

import org.springframework.data.repository.CrudRepository;

import rastreia.lti.web.models.Motorista;

public interface MotoristaRepository extends CrudRepository<Motorista, Long> {
    Iterable<Motorista> findAllByAtivo(boolean ativo);

}
