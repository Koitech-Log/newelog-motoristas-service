// Espelha exatamente os DTOs Java do motoristas-service
// (br.com.newelog.motoristas.dto) — qualquer mudança de contrato no back-end
// deve ser refletida aqui.

export type StatusMotorista = "DISPONIVEL" | "EM_OPERACAO";

export interface MotoristaResumo {
  id: number;
  codigoExterno: string;
  nome: string;
  placaVeiculo: string | null;
  tipoVeiculo: string | null;
  status: StatusMotorista;
  diasDisponiveis: number;
  diasOperacao: number;
  totalViagens: number;
  cadastroValidado: boolean;
}

export interface Viagem {
  data: string; // ISO (YYYY-MM-DD), como serializado pelo LocalDate do Java
  origem: string | null;
  destino: string;
  valorFrete: number;
  valorPedagio: number;
}

export interface MotoristaDetalhe {
  id: number;
  codigoExterno: string;
  nome: string;
  placaVeiculo: string | null;
  tipoVeiculo: string | null;
  marcaVeiculo: string | null;
  status: StatusMotorista;
  diasDisponiveis: number;
  diasOperacao: number;
  valorFreteTotal: number;
  valorPedagioTotal: number;
  cadastroValidado: boolean;
  viagens: Viagem[];
}

export interface ApiErro {
  timestamp: string;
  mensagem: string;
}
