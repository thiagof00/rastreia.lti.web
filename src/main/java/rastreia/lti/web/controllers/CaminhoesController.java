package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CaminhoesController {

    @GetMapping("/caminhoes")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("caminhoes/index");
        mv.addObject("paginaAtiva", "caminhoes");

        return mv;
    }
}
