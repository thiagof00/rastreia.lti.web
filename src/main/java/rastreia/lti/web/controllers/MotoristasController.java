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

import rastreia.lti.web.models.Motorista;
import rastreia.lti.web.repository.MotoristaRepository;

@Controller
public class MotoristasController {

    @Autowired
    MotoristaRepository motoristaRepository;

    @GetMapping("/motoristas")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("motoristas/index");
        mv.addObject("paginaAtiva", "motoristas");
        mv.addObject("motoristas", motoristaRepository.findAll());
        return mv;
    }

    @GetMapping("/novo-motorista")
    public ModelAndView novoMotorista() {
        ModelAndView mv = new ModelAndView("motorista/formNewMotorista");
        mv.addObject("motoristaForm", new Motorista());
        return mv;
    }

    @PostMapping("/novo-motorista")
    public Object saveMotorista(@ModelAttribute("motoristaForm") Motorista motorista) {
        try {
            motoristaRepository.save(motorista);
            return "redirect:motoristas";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("motoristas/form");
            mv.addObject("paginaAtiva", "motoristas");
            mv.addObject("motoristasForm", motorista);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
    }

    @PostMapping("/excluir-motorista/{id}")
    public String deleteCaminhao(@PathVariable Long id, RedirectAttributes redirect) {

        try {
            motoristaRepository.deleteById(id);
            redirect.addFlashAttribute("sucesso", "Motorista excluído com sucesso.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("erro",
                    "Não é possível excluir: o motorista está vinculado a outros registros.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/motoristas";
    }
}
