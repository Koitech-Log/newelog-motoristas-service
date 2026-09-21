package br.com.newelog.motoristas.dto;

import java.util.List;

/**
 * Envelope de paginação próprio, em vez de serializar org.springframework.data.domain.Page
 * diretamente — evita expor detalhes internos do Spring Data no contrato de API
 * (campos como "pageable", "sort" viram implementação vazada) e mantém os nomes
 * de campo estáveis mesmo se a lib de paginação mudar no futuro.
 */
public record PaginaDTO<T>(
        List<T> conteudo,
        int paginaAtual,
        int totalPaginas,
        long totalElementos,
        int tamanhoPagina
) {
}
