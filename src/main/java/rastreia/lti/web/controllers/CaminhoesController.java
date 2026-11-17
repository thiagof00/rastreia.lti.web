package rastreia.lti.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import rastreia.lti.web.models.Caminhao;
import rastreia.lti.web.repository.CaminhaoRepository;

@Controller
public class CaminhoesController {
    @Autowired
    CaminhaoRepository caminhaoRepository;

    @GetMapping("/caminhoes")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("caminhoes/index");
        Long totalCaminhoes = caminhaoRepository.count();
        mv.addObject("paginaAtiva", "caminhoes");
        mv.addObject("caminhoes", caminhaoRepository.findAll());
        mv.addObject("totalCaminhoes", totalCaminhoes);
        return mv;
    }

    @GetMapping("/novo-caminhao")
    public ModelAndView newCaminhao() {
        ModelAndView mv = new ModelAndView("caminhoes/formNewCaminhao");
        mv.addObject("caminhaoForm", new Caminhao());
        return mv;
    }

    @PostMapping("/novo-caminhao")
    public Object saveCaminhao(@ModelAttribute("caminhaoForm") Caminhao caminhao) {

        try {
            caminhaoRepository.save(caminhao);
            return "redirect:/caminhoes";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("caminhoes/formNewCaminhao");
            mv.addObject("paginaAtiva", "caminhoes");
            mv.addObject("caminhaoForm", caminhao);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;

        }

    }

    @GetMapping("/editar-caminhao/{id}")
    public ModelAndView alterarCaminhao(@PathVariable Long id) {
        ModelAndView mv = new ModelAndView("caminhoes/formNewCaminhao");
        Caminhao caminhao = caminhaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        mv.addObject("caminhaoForm", caminhao);
        return mv;
    }

    @PostMapping("/editar-caminhao/{id}")
    public Object alterarCaminhao(@PathVariable Long id, @ModelAttribute Caminhao dados) {
        Caminhao caminhao = caminhaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        caminhao.setPlaca(dados.getPlaca());
        caminhao.setMarca(dados.getMarca());
        caminhao.setModelo(dados.getModelo());

        try {
            caminhaoRepository.save(caminhao);
            return "redirect:/caminhoes";
        } catch (Exception e) {
            ModelAndView mv = new ModelAndView("caminhoes/form");
            mv.addObject("paginaAtiva", "caminhoes");
            mv.addObject("caminhaoForm", caminhao);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;
        }
    }

    @PostMapping("/excluir-caminhao/{id}")
    public String deleteCaminhao(@PathVariable Long id, RedirectAttributes redirect) {

        try {
            caminhaoRepository.deleteById(id);
            redirect.addFlashAttribute("sucesso", "Caminhão excluído com sucesso.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("erro", "Não é possível excluir: o caminhão está vinculado a outros registros.");
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/caminhoes";
    }

}
