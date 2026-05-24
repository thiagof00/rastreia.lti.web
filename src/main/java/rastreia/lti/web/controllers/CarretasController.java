package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CarretasController {

    @GetMapping("/carretas")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("carretas/index");
        mv.addObject("paginaAtiva", "carretas");

        return mv;
    }
}
