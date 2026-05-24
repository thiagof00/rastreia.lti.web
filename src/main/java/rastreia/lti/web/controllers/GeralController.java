package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.qos.logback.core.model.Model;

@Controller
public class GeralController {

    @GetMapping("/dashboard")
    public ModelAndView dashboard() {
        ModelAndView mv = new ModelAndView("dashboard");
        mv.addObject("paginaAtiva", "dashboard");

        return mv;

    }

    @GetMapping("/")
    public ModelAndView login() {
        ModelAndView mv = new ModelAndView("login");

        return mv;
    }

    @PostMapping("/login")
    public String autenticar(
            @RequestParam String usuario,
            @RequestParam String password,
            Model model) {

        // validação simples — substitua pela lógica real do seu sistema
        if (usuario.equals("0")) {
            // usuário digitou 0 — encerrar sistema (ou redirecionar)
            return "redirect:/login";
        }

        if (usuario.isBlank() || password.isBlank()) {
            return "login"; // volta para a tela de login com erro
        }

        // autenticação bem-sucedida → redireciona para o dashboard
        return "redirect:/dashboard";
    }
}
