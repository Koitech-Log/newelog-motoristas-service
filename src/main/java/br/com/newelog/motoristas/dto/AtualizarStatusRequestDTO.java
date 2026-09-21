package br.com.newelog.motoristas.dto;

import br.com.newelog.motoristas.model.StatusMotorista;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo esperado por PATCH /api/motoristas/{id}/status.
 * Ação permitida tanto para Operador quanto Gestor, conforme definição do
 * parceiro (documento "Respostas do Parceiro", seção 1.4) — quem decide se
 * o perfil pode chamar este endpoint é o gateway/serviço de autenticação,
 * não este serviço.
 */
public record AtualizarStatusRequestDTO(
        @NotNull(message = "status é obrigatório")
        StatusMotorista status
) {
}
