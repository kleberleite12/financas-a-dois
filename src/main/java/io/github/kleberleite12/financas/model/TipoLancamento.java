package io.github.kleberleite12.financas.model;

public enum TipoLancamento {

    RENDA_PRINCIPAL("Salário"),
    RENDA_EXTRA("Renda extra"),
    CARTAO_CREDITO("Cartão de crédito"),
    OUTRO_GASTO("Outro gasto"),
    GUARDADO("Valor guardado"),

    // Tipos antigos mantidos para compatibilidade
    RECEITA("Salário"),
    GASTO("Cartão de crédito");

    private final String descricao;

    TipoLancamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}