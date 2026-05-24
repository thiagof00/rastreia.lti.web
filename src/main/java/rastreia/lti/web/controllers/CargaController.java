package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CargaController {

    @GetMapping("/cargas")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("cargas/index");
        mv.addObject("paginaAtiva", "cargas");

        return mv;
    }
}
