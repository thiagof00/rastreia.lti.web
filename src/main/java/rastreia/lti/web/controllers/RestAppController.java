package rastreia.lti.web.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import rastreia.lti.web.service.AutenticacaoService;
import rastreia.lti.web.service.LimitadorDeRequisicoes;

@RestController
@RequestMapping("/api")
public class RestAppController {

    private final AutenticacaoService autenticacaoService;
    private final LimitadorDeRequisicoes limitadorDeRequisicoes;

    public record LoginRequest(String usuario, String senha) {

    }

    public record TokenResponse(String token) {

    }

    public RestAppController(AutenticacaoService autenticacaoService, LimitadorDeRequisicoes limitadorDeRequisicoes) {
        this.autenticacaoService = autenticacaoService;
        this.limitadorDeRequisicoes = limitadorDeRequisicoes;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (limitadorDeRequisicoes.excedeuLimite(GeralController.obterIp(request))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        if (req.usuario() == null || req.usuario().isBlank() || req.senha() == null || req.senha().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (autenticacaoService.estaBloqueado(req.usuario())) {
            return ResponseEntity.status(HttpStatus.LOCKED).build();
        }

        String token = autenticacaoService.login(req.usuario(), req.senha());

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String header) {
        autenticacaoService.logout(header.substring(7).trim());
        return ResponseEntity.noContent().build();
    }

}
