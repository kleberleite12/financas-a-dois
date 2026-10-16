package io.github.kleberleite12.financas.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "saldos_conta",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "responsavel",
                                "periodo"
                        }
                )
        }
)
public class SaldoConta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * Pessoa a quem pertence o saldo.
     *
     * Ex.:
     * Kleber
     * Giovanna
     */
    @Column(nullable = false)
    private String responsavel;


    /*
     * Mês de referência.
     *
     * Sempre será salvo como o primeiro
     * dia do mês.
     *
     * Ex.:
     * 2026-10-01
     */
    @Column(nullable = false)
    private LocalDate periodo;


    /*
     * Dinheiro existente na conta
     * no início daquele mês.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal valorInicial;


    public SaldoConta() {
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getResponsavel() {
        return responsavel;
    }


    public void setResponsavel(
            String responsavel) {

        this.responsavel = responsavel;
    }


    public LocalDate getPeriodo() {
        return periodo;
    }


    public void setPeriodo(
            LocalDate periodo) {

        this.periodo = periodo;
    }


    public BigDecimal getValorInicial() {
        return valorInicial;
    }


    public void setValorInicial(
            BigDecimal valorInicial) {

        this.valorInicial = valorInicial;
    }
}