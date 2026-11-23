package rastreia.lti.web.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import rastreia.lti.web.models.enums.Status_veiculo;

@Entity
@SQLDelete(sql = "UPDATE carretas SET ativo = false WHERE id = ?")
@Table(name = "carretas")
public class Carreta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "placa", length = 100)
    private String placa;

    @Column(name = "tipo", length = 100)
    private String tipo;

    @Column(name = "peso_max", length = 6)
    private Double pesoMax;

    @Column(name = "metragem", length = 6)
    private Double metragem;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status_veiculo status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public Double getMetragem() {
        return metragem;
    }

    public Double getPesoMax() {
        return pesoMax;
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipo() {
        return tipo;
    }

    public Status_veiculo getStatus() {
        return status;
    }

    public void setStatus(Status_veiculo status) {
        this.status = status;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMetragem(Double metragem) {
        this.metragem = metragem;
    }

    public void setPesoMax(Double pesoMax) {
        this.pesoMax = pesoMax;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

}
