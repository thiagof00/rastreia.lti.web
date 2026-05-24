package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MotoristasController {

    @GetMapping("/motoristas")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView("motoristas/index");
        mv.addObject("paginaAtiva", "motoristas");

        return mv;
    }
}
