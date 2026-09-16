import { useCallback, useEffect, useState } from "react";
import { motoristasApi, type FiltrosMotorista } from "../api/motoristas";
import { ApiRequestError } from "../api/client";
import type { MotoristaResumo } from "../types/motorista";

interface EstadoListaMotoristas {
  motoristas: MotoristaResumo[];
  carregando: boolean;
  erro: string | null;
}

export function useMotoristas(filtros: FiltrosMotorista) {
  const [estado, setEstado] = useState<EstadoListaMotoristas>({
    motoristas: [],
    carregando: true,
    erro: null,
  });

  // string estável para não disparar o efeito a cada novo objeto `filtros`
  // recriado no componente pai a cada render.
  const chaveFiltros = JSON.stringify(filtros);

  const carregar = useCallback(async () => {
    setEstado((atual) => ({ ...atual, carregando: true, erro: null }));
    try {
      const motoristas = await motoristasApi.listar(filtros);
      setEstado({ motoristas, carregando: false, erro: null });
    } catch (erro) {
      const mensagem =
        erro instanceof ApiRequestError
          ? erro.message
          : "Não foi possível conectar ao motoristas-service. Ele está rodando em localhost:8080?";
      setEstado({ motoristas: [], carregando: false, erro: mensagem });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chaveFiltros]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  return { ...estado, recarregar: carregar };
}
