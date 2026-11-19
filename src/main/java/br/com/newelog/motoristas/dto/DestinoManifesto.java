package br.com.newelog.motoristas.dto;

/** Normalização dos textos opcionais do manifesto (destino e número), comum aos dois DTOs. */
final class DestinoManifesto {

    private DestinoManifesto() {
    }

    /** Remove espaços das pontas; vazio ou só espaços vira null (= não informado). */
    static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return texto.strip();
    }
}
