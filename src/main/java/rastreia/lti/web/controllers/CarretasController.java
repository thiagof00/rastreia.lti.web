package rastreia.lti.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rastreia.lti.web.models.Carreta;
import rastreia.lti.web.repository.CarretasRepository;

@Controller
public class CarretasController {

    @Autowired
    CarretasRepository carretasRepository;

    @GetMapping("/carretas")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("carretas/index");
        mv.addObject("paginaAtiva", "carretas");
        mv.addObject("carretas", carretasRepository.findAll());

        return mv;
    }

    @GetMapping("/nova-carreta")
    public ModelAndView newCarreta() {
        ModelAndView mv = new ModelAndView("carretas/formNewCarreta");
        mv.addObject("carretaForm", new Carreta());
        return mv;
    }

    @PostMapping("/nova-carreta")
    public Object saveCarreta(@ModelAttribute("carretaForm") Carreta carreta) {

        try {
            carretasRepository.save(carreta);
            return "redirect:/carretas";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("carretas/form");
            mv.addObject("paginaAtiva", "carretas");
            mv.addObject("carretasForm", carreta);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
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

}
