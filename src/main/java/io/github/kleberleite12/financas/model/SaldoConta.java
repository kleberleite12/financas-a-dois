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


    // =========================================
    // RESPONSÁVEL
    // =========================================

    @Column(nullable = false)
    private String responsavel;


    // =========================================
    // PERÍODO
    // =========================================

    /*
     * Sempre salvo como o primeiro
     * dia do mês.
     *
     * Exemplo:
     * 2026-10-01
     */
    @Column(nullable = false)
    private LocalDate periodo;


    // =========================================
    // SALDO INICIAL
    // =========================================

    /*
     * Dinheiro que já existia na conta
     * no início daquele mês.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal valorInicial;


    // =========================================
    // TRANSFERIDO DAS RECEITAS
    // =========================================

    /*
     * Dinheiro que sobrou das receitas
     * do mês e foi movido para
     * "Saldo na conta".
     *
     * Exemplo:
     *
     * Disponível das receitas: R$ 325,22
     *
     * Transfere R$ 325,22
     *
     * Disponível das receitas: R$ 0,00
     * Saldo na conta: + R$ 325,22
     *
     * O total disponível não muda.
     *
     * Não usamos nullable = false aqui
     * para manter compatibilidade com
     * registros antigos já existentes
     * no banco.
     */
    @Column(
            name = "valor_transferido",
            precision = 10,
            scale = 2
    )
    private BigDecimal valorTransferido =
            BigDecimal.ZERO;


    // =========================================
    // CONSTRUTOR
    // =========================================

    public SaldoConta() {
    }


    // =========================================
    // ID
    // =========================================

    public Long getId() {

        return id;
    }


    public void setId(
            Long id) {

        this.id =
                id;
    }


    // =========================================
    // RESPONSÁVEL
    // =========================================

    public String getResponsavel() {

        return responsavel;
    }


    public void setResponsavel(
            String responsavel) {

        this.responsavel =
                responsavel;
    }


    // =========================================
    // PERÍODO
    // =========================================

    public LocalDate getPeriodo() {

        return periodo;
    }


    public void setPeriodo(
            LocalDate periodo) {

        this.periodo =
                periodo;
    }


    // =========================================
    // SALDO INICIAL
    // =========================================

    public BigDecimal getValorInicial() {

        return valorInicial;
    }


    public void setValorInicial(
            BigDecimal valorInicial) {

        this.valorInicial =
                valorInicial;
    }


    // =========================================
    // VALOR TRANSFERIDO
    // =========================================

    public BigDecimal getValorTransferido() {

        /*
         * Registros antigos do banco
         * podem possuir NULL nesse campo.
         *
         * Para eles, consideramos zero.
         */
        if (valorTransferido == null) {

            return BigDecimal.ZERO;
        }


        return valorTransferido;
    }


    public void setValorTransferido(
            BigDecimal valorTransferido) {

        if (valorTransferido == null) {

            this.valorTransferido =
                    BigDecimal.ZERO;

        } else {

            this.valorTransferido =
                    valorTransferido;
        }
    }
}