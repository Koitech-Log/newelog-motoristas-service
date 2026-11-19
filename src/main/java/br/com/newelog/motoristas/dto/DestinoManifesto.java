package br.com.newelog.motoristas.dto;

/** Normalização do destino recebido do manifesto, comum aos dois DTOs. */
final class DestinoManifesto {

    private DestinoManifesto() {
    }

    /** Remove espaços das pontas; vazio ou só espaços vira null (= não informado). */
    static String normalizar(String destino) {
        if (destino == null || destino.isBlank()) return null;
        return destino.strip();
    }
}
