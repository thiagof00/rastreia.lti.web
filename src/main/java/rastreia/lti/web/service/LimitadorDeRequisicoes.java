package rastreia.lti.web.service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class LimitadorDeRequisicoes {

    private static final int MAX_TENTATIVAS_POR_IP = 15;
    private static final long MINUTOS_JANELA = 5;

    private final ConcurrentHashMap<String, Contador> contadores = new ConcurrentHashMap<>();

    public boolean excedeuLimite(String ip) {
        Contador contador = contadores.computeIfAbsent(ip, k -> new Contador());

        synchronized (contador) {
            if (contador.inicioJanela.isBefore(LocalDateTime.now().minusMinutes(MINUTOS_JANELA))) {
                contador.tentativas.set(0);
                contador.inicioJanela = LocalDateTime.now();
            }

            return contador.tentativas.incrementAndGet() > MAX_TENTATIVAS_POR_IP;
        }
    }

    private static class Contador {
        AtomicInteger tentativas = new AtomicInteger(0);
        LocalDateTime inicioJanela = LocalDateTime.now();
    }
}
