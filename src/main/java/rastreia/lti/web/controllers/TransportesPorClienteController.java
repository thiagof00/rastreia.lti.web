package rastreia.lti.web.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import rastreia.lti.web.models.TipoUsuario;
import rastreia.lti.web.models.Usuario;

@RestController
@RequestMapping("/relatorios/transportes-por-cliente")
public class TransportesPorClienteController {

    private static final Set<String> STATUS = Set.of("aberto", "em_andamento", "entregue", "cancelado");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final NamedParameterJdbcTemplate jdbc;

    public TransportesPorClienteController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/clientes")
    public List<ClienteOpcao> clientes(@RequestAttribute Usuario usuarioLogado) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT id, COALESCE(NULLIF(nome_fantasia, ''), NULLIF(razao_social, ''), CONCAT('Cliente #', id)) AS nome
                FROM clientes
                ORDER BY nome, id
                """, (rs, linha) -> new ClienteOpcao(rs.getLong("id"), rs.getString("nome")));
    }

    @GetMapping
    public List<TransporteLinha> gerar(@RequestAttribute Usuario usuarioLogado,
            @RequestParam(required = false) Long cliente,
            @RequestParam(defaultValue = "todo") String periodo,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String origem,
            @RequestParam(defaultValue = "") String destino) {
        verificarAcesso(usuarioLogado);
        try {
            switch (periodo) {
                case "todo" -> { inicio = null; fim = null; }
                case "mes" -> {
                    YearMonth mesAno = YearMonth.parse(mes == null ? "" : mes);
                    inicio = mesAno.atDay(1);
                    fim = mesAno.atEndOfMonth();
                }
                case "intervalo" -> {
                    if (inicio == null || fim == null || fim.isBefore(inicio)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um intervalo válido.");
                    }
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
            }
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um mês válido.");
        }
        if ((!status.isEmpty() && !STATUS.contains(status)) || origem.length() > 100 || destino.length() > 100
                || (inicio != null && (inicio.getYear() < 1000 || fim.getYear() > 9998))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira os valores dos filtros.");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT transporte_id, id_lti,
                       COALESCE(NULLIF(cliente_nome_fantasia, ''), NULLIF(cliente_razao_social, '')) AS cliente,
                       transporte_criado_em, origem, destino_final, peso_total, transporte_status
                FROM vw_transportes_por_cliente
                WHERE 1 = 1
                """);
        var parametros = new MapSqlParameterSource();
        if (cliente != null) {
            sql.append(" AND id_cliente = :cliente");
            parametros.addValue("cliente", cliente);
        }
        if (inicio != null) {
            sql.append(" AND transporte_criado_em >= :inicio AND transporte_criado_em < :fimExclusivo");
            parametros.addValue("inicio", inicio.atStartOfDay());
            parametros.addValue("fimExclusivo", fim.plusDays(1).atStartOfDay());
        }
        if (!status.isEmpty()) {
            sql.append(" AND transporte_status = :status");
            parametros.addValue("status", status);
        }
        if (!origem.isBlank()) {
            sql.append(" AND LOCATE(:origem, origem) > 0");
            parametros.addValue("origem", origem.strip());
        }
        if (!destino.isBlank()) {
            sql.append(" AND LOCATE(:destino, destino_final) > 0");
            parametros.addValue("destino", destino.strip());
        }
        sql.append(" ORDER BY transporte_criado_em DESC, transporte_id DESC");
        return jdbc.query(sql.toString(), parametros, (rs, linha) -> new TransporteLinha(
                rs.getLong("transporte_id"), rs.getString("id_lti"), rs.getString("cliente"),
                rs.getTimestamp("transporte_criado_em").toLocalDateTime().format(DATA_HORA),
                rs.getString("origem"), rs.getString("destino_final"), rs.getBigDecimal("peso_total"),
                rs.getString("transporte_status")));
    }

    private void verificarAcesso(Usuario usuario) {
        if (usuario.getTipo_usuario() != TipoUsuario.ADMIN && usuario.getTipo_usuario() != TipoUsuario.DEV) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public record ClienteOpcao(long id, String nome) {}

    public record TransporteLinha(long id, String lti, String cliente, String criadoEm, String origem,
            String destino, BigDecimal peso, String status) {}
}
