package rastreia.lti.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rastreia.lti.web.interceptor.AutenticacaoInterceptor;
import rastreia.lti.web.service.AutenticacaoService;
import rastreia.lti.web.service.LimitadorDeRequisicoes;

@Controller
public class GeralController {

    private final AutenticacaoService autenticacaoService;
    private final LimitadorDeRequisicoes limitadorDeRequisicoes;

    public GeralController(AutenticacaoService autenticacaoService, LimitadorDeRequisicoes limitadorDeRequisicoes) {
        this.autenticacaoService = autenticacaoService;
        this.limitadorDeRequisicoes = limitadorDeRequisicoes;
    }

    @GetMapping("/dashboard")
    public ModelAndView dashboard() {
        ModelAndView mv = new ModelAndView("dashboard");
        mv.addObject("paginaAtiva", "dashboard");

        return mv;
    }

    @GetMapping("/")
    public ModelAndView login() {
        return new ModelAndView("login");
    }

    @PostMapping("/login")
    public String autenticar(
            @RequestParam String usuario,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        String ip = obterIp(request);

        if (limitadorDeRequisicoes.excedeuLimite(ip)) {
            return "redirect:/?erro=muitas_tentativas";
        }

        if (usuario.isBlank() || password.isBlank()) {
            return "redirect:/?erro";
        }

        if (autenticacaoService.estaBloqueado(usuario)) {
            return "redirect:/?erro=bloqueado";
        }

        String token = autenticacaoService.login(usuario, password);

        if (token == null) {
            return "redirect:/?erro";
        }

        Cookie cookie = new Cookie(AutenticacaoInterceptor.NOME_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(8 * 60 * 60);
        response.addCookie(cookie);

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AutenticacaoInterceptor.NOME_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        autenticacaoService.logout(token);

        Cookie cookie = new Cookie(AutenticacaoInterceptor.NOME_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/";
    }

    private String obterIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
