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

    public static Specification<Motorista> comNomeContendo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String padrao = "%" + texto.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), padrao);
    }

    public static Specification<Motorista> comDestino(String destino) {
        if (destino == null || destino.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Motorista, Viagem> viagens = root.join("viagens");
            return cb.equal(viagens.get("destino"), destino);
        };
    }
}
