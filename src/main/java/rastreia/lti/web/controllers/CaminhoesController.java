package rastreia.lti.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import rastreia.lti.web.models.Caminhao;
import rastreia.lti.web.repository.CaminhaoRepository;

@Controller

public class CaminhoesController {
    @Autowired
    CaminhaoRepository caminhaoRepository;

    @GetMapping("/caminhoes-all")
    public @ResponseBody Iterable<Caminhao> all() {

        return caminhaoRepository.findAll();
    }

    @GetMapping("/caminhoes")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("caminhoes/index");
        mv.addObject("paginaAtiva", "caminhoes");
        mv.addObject("caminhoes", caminhaoRepository.findAll());

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
            ModelAndView mv = new ModelAndView("caminhoes/form");
            mv.addObject("paginaAtiva", "caminhoes");
            mv.addObject("caminhaoForm", caminhao);
            mv.addObject("erro", "Erro ao cadastrar: " + e.getMessage());
            return mv;

        }

    }
}
