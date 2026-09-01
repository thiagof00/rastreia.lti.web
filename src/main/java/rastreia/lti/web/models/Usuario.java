package rastreia.lti.web.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", length = 50)
    private String nome;

    @Column(name = "sobrenome", length = 50)
    private String sobrenome;

    @Column(name = "cpf", length = 14, unique = true, nullable = false)
    private String cpf;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "senha", length = 255, nullable = false)
    private String senha;

    @Column(name = "tipo_usuario", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoUsuario tipo_usuario;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    private Integer tentativasFalhas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    public Boolean getAtivo() {
        return ativo;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSenha() {
        return senha;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public Integer getTentativasFalhas() {
        return tentativasFalhas;
    }

    public TipoUsuario getTipo_usuario() {
        return tipo_usuario;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public void setBloqueadoAte(LocalDateTime bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public void setTentativasFalhas(Integer tentativasFalhas) {
        this.tentativasFalhas = tentativasFalhas;
    }

    public void setTipo_usuario(TipoUsuario tipo_usuario) {
        this.tipo_usuario = tipo_usuario;
    }

}
