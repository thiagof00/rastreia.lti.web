package rastreia.lti.web.repository;

import org.springframework.data.repository.CrudRepository;

import rastreia.lti.web.models.Sessao;

public interface SessaoRepository extends CrudRepository<Sessao, Long> {

    Sessao findByToken(String token);

    void deleteByToken(String token);

}
