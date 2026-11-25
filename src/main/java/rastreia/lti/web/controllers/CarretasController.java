package rastreia.lti.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rastreia.lti.web.models.Carreta;
import rastreia.lti.web.repository.CarretasRepository;

@Controller
public class CarretasController {

    @Autowired
    CarretasRepository carretasRepository;

    @GetMapping("/carretas")
    public ModelAndView index(@RequestParam(defaultValue = "1") int pagina) {
        int numeroPagina = Math.max(1, pagina);

        Page<Carreta> carretasAtivas = carretasRepository.findAllByAtivo(true,
                PageRequest.of(numeroPagina - 1, 10, Sort.by("id")));

        int ultimaPagina = Math.max(1, carretasAtivas.getTotalPages());

        if (numeroPagina > ultimaPagina) {
            return new ModelAndView("redirect:/carretas?pagina=" + ultimaPagina);

        }
        ModelAndView mv = new ModelAndView("carretas/index");
        mv.addObject("paginaAtiva", "carretas");
        mv.addObject("carretas", carretasAtivas.getContent());
        mv.addObject("totalCarretas", carretasAtivas.getTotalElements());
        mv.addObject("paginaCarretas", carretasAtivas);
        mv.addObject("numeroPagina", numeroPagina);
        mv.addObject("primeiroRegistro", carretasAtivas.getPageable().getOffset() + 1);
        mv.addObject("ultimoRegistro",
                carretasAtivas.getPageable().getOffset() + carretasAtivas.getNumberOfElements());

        return mv;
    }

    @GetMapping("/nova-carreta")
    public ModelAndView newCarreta() {
        ModelAndView mv = new ModelAndView("carretas/formNewCarreta");
        mv.addObject("carretaForm", new Carreta());
        return mv;
    }

    @PostMapping("/nova-carreta")
    public Object saveCarreta(@ModelAttribute("carretaForm") Carreta carreta, RedirectAttributes redirect) {

        try {
            carretasRepository.save(carreta);
            redirect.addFlashAttribute("sucesso", "Carreta cadastrada com sucesso!");
            return "redirect:/carretas";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("carretas/formNewCarreta");
            mv.addObject("paginaAtiva", "carretas");
            mv.addObject("carretasForm", carreta);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
    }

    @GetMapping("/editar-carreta/{id}")
    public ModelAndView editarCarreta(@PathVariable Long id) {
        Carreta carreta = carretasRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ModelAndView mv = new ModelAndView("carretas/formNewCarreta");
        mv.addObject("paginaAtiva", "carretas");
        mv.addObject("carretaForm", carreta);
        return mv;
    }

    @PostMapping("/excluir-carreta/{id}")
    public String deleteCaminhao(@PathVariable Long id, RedirectAttributes redirect) {

        try {
            carretasRepository.deleteById(id);
            redirect.addFlashAttribute("sucesso", "Carreta excluído com sucesso.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("erro", "Não é possível excluir: a carreta está vinculado a outros registros.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/carretas";
    }

    @PostMapping("/editar-carreta/{id}")
    public Object editarCarreta(@PathVariable Long id, @ModelAttribute Carreta carreta, RedirectAttributes redirect) {
        ModelAndView mv = new ModelAndView("carretas/formNewCarreta");
        Carreta carretaEncontrada = carretasRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        carretaEncontrada.setPlaca(carreta.getPlaca());
        carretaEncontrada.setMetragem(carreta.getMetragem());
        carretaEncontrada.setPesoMax(carreta.getPesoMax());
        carretaEncontrada.setTipo(carreta.getTipo());
        carretaEncontrada.setStatus(carreta.getStatus());
        System.out.println("id: " + id);
        try {
            carretasRepository.save(carretaEncontrada);
            redirect.addFlashAttribute("sucesso", "Carreta alterada com sucesso");
            return "redirect:/carretas";

        } catch (Exception e) {
            System.out.println("Erro ao alterar carreta id " + carretaEncontrada.getId() + e.getMessage());
            mv.addObject("erro",
                    "Erro ao efetuar alterações na carreta. Por favor, analise os logs" + e.getMessage());
            mv.addObject("carretaForm", carreta);
            return mv;
        }
    }

}
