package io.github.kleberleite12.financas.model;

public enum FormaPagamento {

    // Dinheiro que entrou no próprio mês:
    // salário, renda extra etc.
    RECEITAS_DO_MES(
            "Receitas do mês"
    ),

    // Dinheiro que já está separado
    // no Saldo na conta.
    SALDO_NA_CONTA(
            "Saldo na conta"
    ),

    // Compra ou gasto lançado no cartão.
    CARTAO_CREDITO(
            "Cartão de crédito"
    );


    private final String descricao;


    FormaPagamento(
            String descricao) {

        this.descricao =
                descricao;
    }


    public String getDescricao() {

        return descricao;
    }
}