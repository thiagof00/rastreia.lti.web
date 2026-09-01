package rastreia.lti.web.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rastreia.lti.web.models.Usuario;
import rastreia.lti.web.service.AutenticacaoService;

public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String NOME_COOKIE = "session_token";

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoInterceptor(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = lerCookie(request);
        Usuario usuario = autenticacaoService.validar(token);

        if (usuario == null) {
            response.setHeader("Location", request.getContextPath() + "/");
            response.setStatus(HttpServletResponse.SC_FOUND);
            return false;
        }

        request.setAttribute("usuarioLogado", usuario);
        return true;
    }

    private String lerCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (NOME_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
