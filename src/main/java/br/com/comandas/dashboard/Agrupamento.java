package br.com.comandas.dashboard;

public enum Agrupamento {
    DIA("day"), SEMANA("week"), MES("month");
    private final String unidade;
    Agrupamento(String unidade) { this.unidade = unidade; }
    public String unidade() { return unidade; }
}

