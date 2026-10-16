package io.github.kleberleite12.financas.model;

public enum FormaPagamento {

    SALDO_NA_CONTA("Saldo na conta"),

    CARTAO_CREDITO("Cartão de crédito");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}