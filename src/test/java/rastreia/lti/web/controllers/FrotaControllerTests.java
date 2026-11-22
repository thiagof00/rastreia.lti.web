package rastreia.lti.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import rastreia.lti.web.models.Caminhao;
import rastreia.lti.web.models.Carreta;
import rastreia.lti.web.models.Motorista;
import rastreia.lti.web.repository.CaminhaoRepository;
import rastreia.lti.web.repository.CarretasRepository;
import rastreia.lti.web.repository.MotoristaRepository;

class FrotaControllerTests {

    // Confirma as rotas de cadastro, listagem, edição e inativação de caminhoes.
    @Test
    void fluxoDeCaminhoes() throws Exception {
        var repository = new RepositorioSimulado<Caminhao>();
        var controller = new CaminhoesController();
        controller.caminhaoRepository = repository.criar(CaminhaoRepository.class);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        cadastrarEListar(mvc, repository, "caminhoes", "novo-caminhao", "marca", "Scania");
        assertEquals("Scania", repository.registro.getMarca());

        mvc.perform(post("/editar-caminhao/1").param("marca", "Volvo"))
                .andExpect(redirectedUrl("/caminhoes"));
        assertEquals("Volvo", repository.registro.getMarca());
        assertEquals(2, repository.gravacoes);

        mvc.perform(post("/excluir-caminhao/1"))
                .andExpect(redirectedUrl("/caminhoes"));
        assertEquals(1L, repository.idParaInativar);
    }

    // Confirma as rotas de cadastro, listagem, edição e inativação de carretas.
    @Test
    void fluxoDeCarretas() throws Exception {
        var repository = new RepositorioSimulado<Carreta>();
        var controller = new CarretasController();
        controller.carretasRepository = repository.criar(CarretasRepository.class);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        cadastrarEListar(mvc, repository, "carretas", "nova-carreta", "placa", "AAA1A11");
        assertEquals("AAA1A11", repository.registro.getPlaca());

        mvc.perform(post("/editar-carreta/1").param("placa", "BBB2B22"))
                .andExpect(redirectedUrl("/carretas"));
        assertEquals("BBB2B22", repository.registro.getPlaca());
        assertEquals(2, repository.gravacoes);

        mvc.perform(post("/excluir-carreta/1"))
                .andExpect(redirectedUrl("/carretas"));
        assertEquals(1L, repository.idParaInativar);
    }

    // Confirma as rotas de cadastro, listagem, edição e inativação de motoristas.
    @Test
    void fluxoDeMotoristas() throws Exception {
        var repository = new RepositorioSimulado<Motorista>();
        var controller = new MotoristasController();
        controller.motoristaRepository = repository.criar(MotoristaRepository.class);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        cadastrarEListar(mvc, repository, "motoristas", "novo-motorista", "nome", "Ana");
        assertEquals("Ana", repository.registro.getNome());

        mvc.perform(post("/editar-motorista/1").param("nome", "Maria"))
                .andExpect(redirectedUrl("/motoristas"));
        assertEquals("Maria", repository.registro.getNome());
        assertEquals(2, repository.gravacoes);

        mvc.perform(post("/excluir-motorista/1"))
                .andExpect(redirectedUrl("/motoristas"));
        assertEquals(1L, repository.idParaInativar);
    }

    private void cadastrarEListar(MockMvc mvc, RepositorioSimulado<?> repository,
            String modulo, String rota, String campo, String valor) throws Exception {
        mvc.perform(post("/" + rota).param(campo, valor))
                .andExpect(redirectedUrl("/" + modulo));
        assertNotNull(repository.registro);
        mvc.perform(get("/" + modulo))
                .andExpect(status().isOk())
                .andExpect(model().attribute(modulo, List.of(repository.registro)));
        assertEquals(Boolean.TRUE, repository.filtroAtivo);
    }

    private static class RepositorioSimulado<T> {
        private T registro;
        private int gravacoes;
        private Boolean filtroAtivo;
        private Long idParaInativar;

        @SuppressWarnings("unchecked")
        private <R> R criar(Class<R> tipo) {
            return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo},
                    (proxy, metodo, args) -> switch (metodo.getName()) {
                        case "save" -> {
                            registro = (T) args[0];
                            gravacoes++;
                            yield registro;
                        }
                        case "findById" -> Long.valueOf(1).equals(args[0])
                                ? Optional.ofNullable(registro) : Optional.empty();
                        case "count", "countByAtivo" -> registro == null ? 0L : 1L;
                        case "findAllByAtivo" -> {
                            filtroAtivo = (Boolean) args[0];
                            yield List.of(registro);
                        }
                        case "deleteById" -> {
                            idParaInativar = (Long) args[0];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(metodo.getName());
                    }));
        }
    }
}
