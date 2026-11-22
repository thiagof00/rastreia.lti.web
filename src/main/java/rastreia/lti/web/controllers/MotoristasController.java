package rastreia.lti.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
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
        Long totalMotoristas = motoristaRepository.count();

        mv.addObject("paginaAtiva", "motoristas");
        mv.addObject("motoristas", motoristaRepository.findAllByAtivo(true));
        mv.addObject("totalMotoristas", totalMotoristas);
        return mv;
    }

    @GetMapping("/novo-motorista")
    public ModelAndView novoMotorista() {
        ModelAndView mv = new ModelAndView("motoristas/formNewMotorista");
        mv.addObject("motoristaForm", new Motorista());
        return mv;
    }

    @PostMapping("/novo-motorista")
    public Object saveMotorista(@ModelAttribute("motoristaForm") Motorista motorista) {
        try {
            motoristaRepository.save(motorista);
            return "redirect:/motoristas";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("motoristas/formNewMotorista");
            mv.addObject("paginaAtiva", "motoristas");
            mv.addObject("motoristaForm", motorista);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
    }

    @GetMapping("/editar-motorista/{id}")
    public ModelAndView editarMotorista(@PathVariable Long id) {
        ModelAndView mv = new ModelAndView("motoristas/formNewMotorista");
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        mv.addObject("motoristaForm", motorista);
        return mv;
    }

    @PostMapping("/editar-motorista/{id}")
    public Object editarMotorista(@PathVariable Long id, @ModelAttribute Motorista motorista,
            RedirectAttributes redirect) {
        ModelAndView mv = new ModelAndView("motoristas/formNewMotorista");
        Motorista motoristaEncontrado = motoristaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        motoristaEncontrado.setNome(motorista.getNome());
        motoristaEncontrado.setSobrenome(motorista.getSobrenome());
        motoristaEncontrado.setCpf(motorista.getCpf());
        motoristaEncontrado.setCnh(motorista.getCnh());
        System.out.println("id: " + id);
        try {
            motoristaRepository.save(motoristaEncontrado);
            redirect.addFlashAttribute("sucesso", "Motorista alterado com sucesso");
            return "redirect:/motoristas";

        } catch (Exception e) {
            System.out.println("Erro ao alterar motorista id " + motoristaEncontrado.getId() + e.getMessage());
            mv.addObject("erro",
                    "Erro ao alterar motorista.");
            mv.addObject("motoristaForm", motoristaEncontrado);
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
