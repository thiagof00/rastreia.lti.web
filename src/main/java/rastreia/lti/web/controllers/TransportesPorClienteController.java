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

    @GetMapping("/etapas")
    public List<EtapaLinha> etapas(@RequestAttribute Usuario usuarioLogado, @RequestParam long transporteId) {
        verificarAcesso(usuarioLogado);
        var parametros = parametrosDoTransporte(transporteId);
        return jdbc.query("""
                SELECT id, observacoes, criado_em, atualizado_em
                FROM etapas_transporte
                WHERE id_transporte = :transporteId
                ORDER BY criado_em, id
                """, parametros, (rs, linha) -> new EtapaLinha(rs.getLong("id"), rs.getString("observacoes"),
                        rs.getObject("criado_em", java.time.LocalDateTime.class),
                        rs.getObject("atualizado_em", java.time.LocalDateTime.class)));
    }

    @GetMapping("/pedidos")
    public List<PedidoLinha> pedidos(@RequestAttribute Usuario usuarioLogado, @RequestParam long transporteId) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT p.id, p.numero, p.serie, p.data_emissao
                FROM transporte_pedido tp
                JOIN pedido p ON p.id = tp.id_pedido
                WHERE tp.id_transporte = :transporteId
                ORDER BY p.data_emissao DESC, p.id DESC
                """, parametrosDoTransporte(transporteId), (rs, linha) -> new PedidoLinha(
                        rs.getLong("id"), rs.getString("numero"), rs.getString("serie"),
                        rs.getObject("data_emissao", LocalDate.class)));
    }

    @GetMapping("/nfe")
    public List<NotaFiscalLinha> notasFiscais(@RequestAttribute Usuario usuarioLogado, @RequestParam long transporteId) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT n.id, n.numero, n.serie
                FROM transporte_nfe tn
                JOIN nfe n ON n.id = tn.id_nfe
                WHERE tn.id_transporte = :transporteId
                ORDER BY n.id DESC
                """, parametrosDoTransporte(transporteId), (rs, linha) -> new NotaFiscalLinha(
                        rs.getLong("id"), rs.getString("numero"), rs.getString("serie")));
    }

    public record NotaFiscalLinha(long id, String numero, String serie) {}

    private MapSqlParameterSource parametrosDoTransporte(long transporteId) {
        if (transporteId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um ID de transporte válido.");
        }
        var parametros = new MapSqlParameterSource("transporteId", transporteId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM transporte WHERE id = :transporteId", parametros, Integer.class) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporte não encontrado.");
        }
        return parametros;
    }

    public record PedidoLinha(long id, String numero, String serie, LocalDate dataEmissao) {}

    @GetMapping("/motoristas")
    public List<ClienteOpcao> motoristas(@RequestAttribute Usuario usuarioLogado) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT id, CONCAT_WS(' ', nome, sobrenome) AS nome FROM motoristas ORDER BY nome, id
                """, (rs, linha) -> new ClienteOpcao(rs.getLong("id"), rs.getString("nome")));
    }

    @GetMapping("/etapas-motorista")
    public List<EtapaMotorista> etapasMotorista(@RequestAttribute Usuario usuarioLogado,
            @RequestParam long motoristaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        verificarAcesso(usuarioLogado);
        return consultarEtapas("e.id_motorista", motoristaId, inicio, fim);
    }

    @GetMapping("/caminhoes")
    public List<ClienteOpcao> caminhoes(@RequestAttribute Usuario usuarioLogado) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT id, CONCAT_WS(' · ', placa, NULLIF(modelo, '')) AS nome
                FROM caminhoes ORDER BY placa, id
                """, (rs, linha) -> new ClienteOpcao(rs.getLong("id"), rs.getString("nome")));
    }

    @GetMapping("/etapas-caminhao")
    public List<EtapaMotorista> etapasCaminhao(@RequestAttribute Usuario usuarioLogado,
            @RequestParam long caminhaoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        verificarAcesso(usuarioLogado);
        return consultarEtapas("e.id_caminhao", caminhaoId, inicio, fim);
    }

    @GetMapping("/carretas")
    public List<ClienteOpcao> carretas(@RequestAttribute Usuario usuarioLogado) {
        verificarAcesso(usuarioLogado);
        return jdbc.query("""
                SELECT id, CONCAT_WS(' · ', placa, NULLIF(tipo, '')) AS nome
                FROM carretas ORDER BY placa, id
                """, (rs, linha) -> new ClienteOpcao(rs.getLong("id"), rs.getString("nome")));
    }

    @GetMapping("/etapas-carreta")
    public List<EtapaMotorista> etapasCarreta(@RequestAttribute Usuario usuarioLogado,
            @RequestParam long carretaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        verificarAcesso(usuarioLogado);
        return consultarEtapas("e.id_carreta", carretaId, inicio, fim);
    }

    private List<EtapaMotorista> consultarEtapas(String coluna, long id, LocalDate inicio, LocalDate fim) {
        if (id <= 0 || (inicio == null) != (fim == null)
                || (inicio != null && (fim.isBefore(inicio) || inicio.getYear() < 1000 || fim.getYear() > 9998))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira a seleção e o período.");
        }
        var parametros = new MapSqlParameterSource("id", id);
        String sql = """
                SELECT e.id, e.id_transporte, e.observacoes, e.criado_em, t.status
                FROM etapas_transporte e
                LEFT JOIN transporte t ON t.id = e.id_transporte
                WHERE %s = :id
                """.formatted(coluna);
        if (inicio != null) {
            sql += " AND e.criado_em >= :inicio AND e.criado_em < :fim";
            parametros.addValue("inicio", inicio.atStartOfDay()).addValue("fim", fim.plusDays(1).atStartOfDay());
        }
        return jdbc.query(sql + " ORDER BY e.criado_em DESC, e.id DESC", parametros, (rs, linha) -> {
            var criado = rs.getObject("criado_em", java.time.LocalDateTime.class);
            return new EtapaMotorista(rs.getLong("id"), rs.getObject("id_transporte", Long.class),
                    rs.getString("observacoes"), criado == null ? null : criado.format(DATA_HORA), rs.getString("status"));
        });
    }

    public record EtapaMotorista(long id, Long transporteId, String observacoes, String criadoEm, String status) {}

    public record EtapaLinha(long id, String observacoes, java.time.LocalDateTime criadoEm,
            java.time.LocalDateTime atualizadoEm) {}

    @GetMapping("/tabelas/{tipo}")
    public TabelaRelatorio tabela(@RequestAttribute Usuario usuarioLogado,
            @org.springframework.web.bind.annotation.PathVariable String tipo,
            @RequestParam(required = false) Long transporteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        verificarAcesso(usuarioLogado);
        if (Set.of("sessoes", "bloqueios").contains(tipo) && usuarioLogado.getTipo_usuario() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var parametros = new MapSqlParameterSource();
        String periodo = "";
        if (inicio != null || fim != null) {
            if (inicio == null || fim == null || fim.isBefore(inicio) || inicio.getYear() < 1000 || fim.getYear() > 9998) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um intervalo válido.");
            }
            periodo = " AND t.criado_em >= :inicio AND t.criado_em < :fim";
            parametros.addValue("inicio", inicio.atStartOfDay()).addValue("fim", fim.plusDays(1).atStartOfDay());
        }
        return switch (tipo) {
            case "timeline" -> consultarTabela("""
                    SELECT e.criado_em, e.observacoes, NULLIF(CONCAT_WS(' ', m.nome, m.sobrenome), ''),
                           c.placa, r.placa
                    FROM etapas_transporte e
                    LEFT JOIN motoristas m ON m.id = e.id_motorista
                    LEFT JOIN caminhoes c ON c.id = e.id_caminhao
                    LEFT JOIN carretas r ON r.id = e.id_carreta
                    WHERE e.id_transporte = :transporteId
                    ORDER BY e.criado_em, e.id
                    """, parametrosDoTransporte(exigirTransporte(transporteId)),
                    "Data da etapa", "Observações", "Motorista", "Caminhão", "Carreta");
            case "produtividade" -> consultarTabela("""
                    SELECT CONCAT_WS(' ', m.nome, m.sobrenome), COUNT(e.id), COUNT(DISTINCT e.id_transporte)
                    FROM etapas_transporte e JOIN motoristas m ON m.id = e.id_motorista
                    WHERE 1 = 1
                    """ + periodo.replace("t.criado_em", "e.criado_em") + """
                     GROUP BY m.id, m.nome, m.sobrenome ORDER BY 2 DESC, 1, m.id
                    """, parametros, "Motorista", "Etapas", "Transportes distintos");
            case "utilizacao" -> consultarTabela("""
                    SELECT 'Caminhão' AS tipo, c.placa, COUNT(e.id), COUNT(DISTINCT e.id_transporte)
                    FROM etapas_transporte e JOIN caminhoes c ON c.id = e.id_caminhao
                    WHERE 1 = 1
                    """ + periodo.replace("t.criado_em", "e.criado_em") + """
                     GROUP BY c.id, c.placa
                    UNION ALL
                    SELECT 'Carreta', r.placa, COUNT(e.id), COUNT(DISTINCT e.id_transporte)
                    FROM etapas_transporte e JOIN carretas r ON r.id = e.id_carreta
                    WHERE 1 = 1
                    """ + periodo.replace("t.criado_em", "e.criado_em") + """
                     GROUP BY r.id, r.placa ORDER BY 3 DESC, 1, 2
                    """, parametros, "Veículo", "Placa", "Etapas", "Transportes distintos");
            case "status" -> consultarTabela("""
                    SELECT t.status, COUNT(*)
                    FROM transporte t WHERE 1 = 1
                    """ + periodo + " GROUP BY t.status ORDER BY 2 DESC, 1",
                    parametros, "Status", "Transportes");
            case "ocupacao" -> consultarTabela("""
                    WITH etapas_ordenadas AS (
                        SELECT e.id_transporte, e.id_caminhao, e.id_carreta, e.criado_em,
                               ROW_NUMBER() OVER (PARTITION BY e.id_transporte ORDER BY e.criado_em DESC, e.id DESC) AS ordem
                        FROM etapas_transporte e
                        JOIN transporte t ON t.id = e.id_transporte AND t.status = 'em_andamento'
                    )
                    SELECT t.id, c.placa, r.placa, t.peso_total, r.peso_max, r.metragem,
                           ROUND(100 * t.peso_total / NULLIF(r.peso_max, 0), 1), e.criado_em
                    FROM transporte t
                    LEFT JOIN etapas_ordenadas e ON e.id_transporte = t.id AND e.ordem = 1
                    LEFT JOIN caminhoes c ON c.id = e.id_caminhao
                    LEFT JOIN carretas r ON r.id = e.id_carreta
                    WHERE t.status = 'em_andamento'
                    ORDER BY t.id DESC
                    """, parametros, "Transporte", "Caminhão", "Carreta", "Peso da carga",
                    "Limite da carreta", "Metragem cadastrada", "Ocupação por peso (%)", "Última etapa");
            case "invoices" -> consultarTabela("""
                    SELECT i.id, i.numero
                    FROM transporte_invoice ti JOIN invoice i ON i.id = ti.id_invoice
                    WHERE ti.id_transporte = :transporteId ORDER BY i.id DESC
                    """, parametrosDoTransporte(exigirTransporte(transporteId)), "ID da invoice", "Número");
            case "combinacoes" -> consultarTabela("""
                    SELECT c.placa, r.placa, COUNT(e.id), COUNT(DISTINCT e.id_transporte), MAX(e.criado_em)
                    FROM etapas_transporte e
                    JOIN caminhoes c ON c.id = e.id_caminhao
                    JOIN carretas r ON r.id = e.id_carreta
                    GROUP BY c.id, c.placa, r.id, r.placa
                    ORDER BY c.placa, r.placa
                    """, parametros, "Caminhão", "Carreta", "Etapas", "Transportes", "Último registro");
            case "documentacao" -> consultarTabela("""
                    SELECT 'Pedido', p.numero, p.serie
                    FROM transporte_pedido tp JOIN pedido p ON p.id = tp.id_pedido
                    WHERE tp.id_transporte = :transporteId
                    UNION ALL
                    SELECT 'NF-e', n.numero, n.serie
                    FROM transporte_nfe tn JOIN nfe n ON n.id = tn.id_nfe
                    WHERE tn.id_transporte = :transporteId
                    UNION ALL
                    SELECT 'Invoice', i.numero, NULL
                    FROM transporte_invoice ti JOIN invoice i ON i.id = ti.id_invoice
                    WHERE ti.id_transporte = :transporteId
                    ORDER BY 1, 2, 3
                    """, parametrosDoTransporte(exigirTransporte(transporteId)), "Documento", "Número", "Série");
            case "peso" -> consultarTabela("""
                    SELECT COALESCE(NULLIF(c.nome_fantasia, ''), c.razao_social, 'Sem cliente'),
                           DATE_FORMAT(t.criado_em, '%m/%Y'), COUNT(*), COUNT(t.peso_total), SUM(t.peso_total)
                    FROM transporte t LEFT JOIN clientes c ON c.id = t.id_cliente
                    WHERE t.status = 'entregue'
                    """ + periodo + """
                     GROUP BY c.id, c.nome_fantasia, c.razao_social, DATE_FORMAT(t.criado_em, '%m/%Y'),
                              YEAR(t.criado_em), MONTH(t.criado_em)
                     ORDER BY YEAR(t.criado_em) DESC, MONTH(t.criado_em) DESC, 1, c.id
                    """, parametros, "Cliente", "Mês de criação", "Entregues", "Com peso informado", "Peso total");
            case "pendencias" -> consultarTabela("""
                    SELECT t.id, t.id_lti, t.origem, t.destino_final,
                           CONCAT_WS(', ',
                               IF(p.id_transporte IS NULL, 'Pedido', NULL),
                               IF(n.id_transporte IS NULL, 'NF-e', NULL),
                               IF(i.id_transporte IS NULL, 'Invoice', NULL))
                    FROM transporte t
                    LEFT JOIN (SELECT DISTINCT id_transporte FROM transporte_pedido) p ON p.id_transporte = t.id
                    LEFT JOIN (SELECT DISTINCT id_transporte FROM transporte_nfe) n ON n.id_transporte = t.id
                    LEFT JOIN (SELECT DISTINCT id_transporte FROM transporte_invoice) i ON i.id_transporte = t.id
                    WHERE t.status <> 'cancelado'
                      AND (p.id_transporte IS NULL OR n.id_transporte IS NULL OR i.id_transporte IS NULL)
                    """ + periodo + " ORDER BY t.criado_em DESC, t.id DESC",
                    parametros, "ID do transporte", "Código LTI", "Origem", "Destino", "Sem vínculo");
            case "sessoes" -> consultarTabela("""
                    SELECT CONCAT_WS(' ', u.nome, u.sobrenome), s.criado_em, s.expira_em,
                           CASE WHEN s.expira_em < CURRENT_TIMESTAMP THEN 'Expirada' ELSE 'Ativa' END
                    FROM sessoes s JOIN usuarios u ON u.id = s.usuario_id
                    ORDER BY s.criado_em DESC, s.id DESC
                    """, parametros, "Usuário", "Criada em", "Expira em", "Situação");
            case "bloqueios" -> consultarTabela("""
                    SELECT CONCAT_WS(' ', nome, sobrenome), tentativas_falhas, bloqueado_ate,
                           CASE WHEN bloqueado_ate > CURRENT_TIMESTAMP THEN 'Bloqueado' ELSE 'Sem bloqueio ativo' END
                    FROM usuarios
                    WHERE tentativas_falhas > 0 OR bloqueado_ate > CURRENT_TIMESTAMP
                    ORDER BY bloqueado_ate DESC, tentativas_falhas DESC, id
                    """, parametros, "Usuário", "Tentativas registradas", "Bloqueado até", "Situação");
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    private long exigirTransporte(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o ID do transporte.");
        }
        return id;
    }

    private TabelaRelatorio consultarTabela(String sql, MapSqlParameterSource parametros, String... colunas) {
        List<List<String>> linhas = jdbc.query(sql, parametros, (rs, linha) -> {
            var valores = new java.util.ArrayList<String>(colunas.length);
            for (int i = 1; i <= colunas.length; i++) {
                Object valor = rs.getObject(i);
                valores.add(valor instanceof java.sql.Timestamp data
                        ? data.toLocalDateTime().format(DATA_HORA) : rs.getString(i));
            }
            return valores;
        });
        return new TabelaRelatorio(List.of(colunas), linhas);
    }

    public record TabelaRelatorio(List<String> colunas, List<List<String>> linhas) {}

    private void verificarAcesso(Usuario usuario) {
        if (usuario.getTipo_usuario() != TipoUsuario.ADMIN && usuario.getTipo_usuario() != TipoUsuario.DEV) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public record ClienteOpcao(long id, String nome) {}

    public record TransporteLinha(long id, String lti, String cliente, String criadoEm, String origem,
            String destino, BigDecimal peso, String status) {}
}
