package br.com.newelog.motoristas.model;

/**
 * Situação atual do motorista quanto à disponibilidade para novas operações.
 * Os nomes ficam em português para bater com a linguagem de negócio usada
 * no restante do domínio (evita tradução mental entre código e conversa com o parceiro).
 */
public enum StatusMotorista {
    DISPONIVEL,
    EM_OPERACAO
}
