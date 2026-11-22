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
        boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");

        String token = api ? lerBearer(request) : lerCookie(request);

        Usuario usuario = autenticacaoService.validar(token);

        if (usuario == null) {
            if (api) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {

                response.setHeader("Location", request.getContextPath() + "/");
                response.setStatus(HttpServletResponse.SC_FOUND);
            }

            return false;
        }

        request.setAttribute("usuarioLogado", usuario);
        return true;
    }

    private String lerBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
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
