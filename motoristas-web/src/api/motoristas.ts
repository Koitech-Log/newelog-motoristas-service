import { apiClient } from "./client";
import type {
  MotoristaDetalhe,
  MotoristaResumo,
  StatusMotorista,
} from "../types/motorista";

export interface FiltrosMotorista {
  status?: StatusMotorista;
  destino?: string;
  busca?: string;
}

function paraQueryString(filtros: FiltrosMotorista): string {
  const params = new URLSearchParams();
  if (filtros.status) params.set("status", filtros.status);
  if (filtros.destino) params.set("destino", filtros.destino);
  if (filtros.busca) params.set("busca", filtros.busca);
  const query = params.toString();
  return query ? `?${query}` : "";
}

export const motoristasApi = {
  listar: (filtros: FiltrosMotorista = {}) =>
    apiClient.get<MotoristaResumo[]>(`/api/motoristas${paraQueryString(filtros)}`),

  buscarDetalhe: (id: number) =>
    apiClient.get<MotoristaDetalhe>(`/api/motoristas/${id}`),

  atualizarStatus: (id: number, status: StatusMotorista) =>
    apiClient.patch<MotoristaDetalhe>(`/api/motoristas/${id}/status`, { status }),
};
