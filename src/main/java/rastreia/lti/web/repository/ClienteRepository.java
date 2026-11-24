package rastreia.lti.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import rastreia.lti.web.models.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

}
