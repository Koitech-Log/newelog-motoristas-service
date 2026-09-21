package br.com.newelog.motoristas.repository;

import org.springframework.data.jpa.domain.Specification;

import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.model.Viagem;
import jakarta.persistence.criteria.Join;

/**
 * Filtros combináveis para a listagem de motoristas — espelham exatamente os
 * filtros já existentes na tela de Motoristas do protótipo front-end (status,
 * destino, busca por nome), para que a migração do mock para esta API não exija
 * mudar a lógica de filtro do lado do cliente.
 */
public final class MotoristaSpecifications {

    private MotoristaSpecifications() {
    }

    public static Specification<Motorista> comStatus(StatusMotorista status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Busca por nome OU CPF/CNPJ, ambos "contains" case-insensitive.
     *
     * O CPF é armazenado formatado (123.456.789-00). Para aceitar tanto a
     * busca formatada quanto só dígitos (ex: usuário digita "12345678900"),
     * a comparação é feita em duas variantes calculadas em Java — evita
     * depender de função de regex do banco, cuja assinatura difere entre
     * PostgreSQL e H2 (usado nos testes) e já causou um bug de portabilidade
     * antes neste serviço (ver comentário sobre SELECT setval() no README).
     */
    public static Specification<Motorista> comNomeOuCpfContendo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String padraoNome = "%" + texto.toLowerCase() + "%";
        String padraoCpfComoDigitado = "%" + texto + "%";

        String soDigitos = texto.replaceAll("[^0-9]", "");
        String cpfReformatado = formatarComoCpf(soDigitos);

        return (root, query, cb) -> {
            var porNome = cb.like(cb.lower(root.get("nome")), padraoNome);
            var porCpfComoDigitado = cb.like(root.get("cpfCnpj"), padraoCpfComoDigitado);
            if (cpfReformatado == null) {
                return cb.or(porNome, porCpfComoDigitado);
            }
            var porCpfReformatado = cb.like(root.get("cpfCnpj"), "%" + cpfReformatado + "%");
            return cb.or(porNome, porCpfComoDigitado, porCpfReformatado);
        };
    }

    /**
     * Reconstrói a pontuação de CPF (000.000.000-00) a partir de uma sequência
     * de dígitos parcial — permite casar "12345678900" ou apenas "123456789"
     * com o valor formatado salvo no banco. Retorna null se não houver dígito
     * algum (nada a reformatar).
     */
    private static String formatarComoCpf(String digitos) {
        if (digitos.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int[] pontosApos = {3, 6};
        for (int i = 0; i < digitos.length() && i < 11; i++) {
            if (i == 9) {
                sb.append('-');
            } else if (i == pontosApos[0] || i == pontosApos[1]) {
                sb.append('.');
            }
            sb.append(digitos.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Filtro por destino — "contains" case-insensitive (não igualdade exata).
     * O campo de UI é texto livre, então o usuário pode digitar só parte do
     * nome do destino (ex.: "paulo" para achar "São Paulo") ou sem acento;
     * comparar com igualdade exata fazia a busca nunca encontrar nada.
     */
    public static Specification<Motorista> comDestino(String destino) {
        if (destino == null || destino.isBlank()) {
            return null;
        }
        String padrao = "%" + destino.toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Motorista, Viagem> viagens = root.join("viagens");
            return cb.like(cb.lower(viagens.get("destino")), padrao);
        };
    }
}
