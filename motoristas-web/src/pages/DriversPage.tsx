import { useMemo, useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useMotoristas } from "../hooks/useMotoristas";
import { DriverFilters } from "../components/DriverFilters";
import { DriverCard } from "../components/DriverCard";
import { DriverDrawer } from "../components/DriverDrawer";
import { icons } from "../components/icons";
import type { StatusMotorista } from "../types/motorista";
import styles from "./DriversPage.module.css";

export function DriversPage() {
  const [busca, setBusca] = useState("");
  const [statusAtivo, setStatusAtivo] = useState<StatusMotorista | "all">("all");
  const [motoristaSelecionado, setMotoristaSelecionado] = useState<number | null>(null);

  const filtros = useMemo(
    () => ({
      status: statusAtivo === "all" ? undefined : statusAtivo,
      busca: busca || undefined,
    }),
    [statusAtivo, busca]
  );

  const { motoristas, carregando, erro, recarregar } = useMotoristas(filtros);

  return (
    <div className={styles.page}>
      <span className={styles.eyebrow}>Controle de disponibilidade</span>
      <h1 className={styles.title}>Motoristas agregados</h1>
      <p className={styles.subtitle}>
        Identifique quem está disponível para uma nova operação e filtre por nome ou status.
      </p>

      <DriverFilters
        busca={busca}
        onBuscaChange={setBusca}
        statusAtivo={statusAtivo}
        onStatusChange={setStatusAtivo}
      />

      {erro && (
        <div className={styles.errorBox}>
          <FontAwesomeIcon icon={icons.error} className={styles.errorIcon} />
          <p>{erro}</p>
          <button type="button" onClick={recarregar} className={styles.retryBtn}>
            <FontAwesomeIcon icon={icons.retry} /> Tentar novamente
          </button>
        </div>
      )}

      {!erro && carregando && (
        <p className={styles.loading}>
          <FontAwesomeIcon icon={icons.spinner} spin size="lg" />
          <span>Carregando motoristas…</span>
        </p>
      )}

      {!erro && !carregando && motoristas.length === 0 && (
        <div className={styles.emptyState}>
          <FontAwesomeIcon icon={icons.inbox} className={styles.emptyIcon} />
          <p>Nenhum motorista corresponde a esses filtros.</p>
        </div>
      )}

      {!erro && !carregando && motoristas.length > 0 && (
        <>
          <p className={styles.resultsMeta}>
            {motoristas.length} motorista{motoristas.length !== 1 ? "s" : ""} encontrado
            {motoristas.length !== 1 ? "s" : ""}
          </p>
          <div className={styles.grid}>
            {motoristas.map((motorista, indice) => (
              <DriverCard
                key={motorista.id}
                motorista={motorista}
                onSelecionar={setMotoristaSelecionado}
                indice={indice}
              />
            ))}
          </div>
        </>
      )}

      <DriverDrawer
        motoristaId={motoristaSelecionado}
        onClose={() => setMotoristaSelecionado(null)}
        aoAtualizarStatus={recarregar}
      />
    </div>
  );
}
