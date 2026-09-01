package rastreia.lti.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import rastreia.lti.web.interceptor.AutenticacaoInterceptor;
import rastreia.lti.web.service.AutenticacaoService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoService autenticacaoService;

    public WebConfig(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AutenticacaoInterceptor(autenticacaoService))
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/logout",
                        "/global.css",
                        "/login.jpg");
    }
}
