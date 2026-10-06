package io.github.kleberleite12.financas.model;

public enum TipoLancamento {

    RENDA_PRINCIPAL("Renda principal"),
    RENDA_EXTRA("Renda extra"),
    CARTAO_CREDITO("Cartão de crédito"),
    OUTRO_GASTO("Outro gasto"),
    GUARDADO("Valor guardado"),

    // Tipos antigos mantidos para não quebrar lançamentos já existentes
    RECEITA("Renda principal"),
    GASTO("Cartão de crédito");

    private final String descricao;

    TipoLancamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}