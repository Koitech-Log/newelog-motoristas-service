import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { icons } from "./icons";
import type { StatusMotorista } from "../types/motorista";
import styles from "./DriverFilters.module.css";

interface DriverFiltersProps {
  busca: string;
  onBuscaChange: (valor: string) => void;
  statusAtivo: StatusMotorista | "all";
  onStatusChange: (status: StatusMotorista | "all") => void;
}

const OPCOES_STATUS: { valor: StatusMotorista | "all"; rotulo: string }[] = [
  { valor: "all", rotulo: "Todos" },
  { valor: "DISPONIVEL", rotulo: "Disponíveis" },
  { valor: "EM_OPERACAO", rotulo: "Em operação" },
];

export function DriverFilters({
  busca,
  onBuscaChange,
  statusAtivo,
  onStatusChange,
}: DriverFiltersProps) {
  return (
    <div className={styles.bar}>
      <div className={styles.searchField}>
        <FontAwesomeIcon icon={icons.search} aria-hidden />
        <input
          type="text"
          placeholder="Buscar por nome…"
          value={busca}
          onChange={(e) => onBuscaChange(e.target.value)}
        />
      </div>

      {OPCOES_STATUS.map((opcao) => (
        <button
          key={opcao.valor}
          type="button"
          className={`${styles.chip} ${statusAtivo === opcao.valor ? styles.active : ""}`}
          onClick={() => onStatusChange(opcao.valor)}
        >
          {opcao.rotulo}
        </button>
      ))}
    </div>
  );
}
