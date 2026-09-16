// Import individual de cada ícone (não o pacote inteiro) para manter o bundle
// enxuto — free-solid-svg-icons sozinho tem centenas de ícones.
import {
  faMagnifyingGlass,
  faTruck,
  faTruckFast,
  faChevronRight,
  faXmark,
  faCircleCheck,
  faCircleExclamation,
  faGaugeHigh,
  faRoute,
  faCalendarCheck,
  faUserPlus,
  faSpinner,
  faInbox,
  faArrowRotateRight,
} from "@fortawesome/free-solid-svg-icons";

export const icons = {
  search: faMagnifyingGlass,
  truck: faTruck,
  truckFast: faTruckFast,
  chevronRight: faChevronRight,
  close: faXmark,
  error: faCircleExclamation,
  userPlus: faUserPlus,
  spinner: faSpinner,
  inbox: faInbox,
  retry: faArrowRotateRight,
  // Reservados para as próximas telas (Home, Ranking) quando os demais
  // microsserviços existirem — ver README, seção "Pendências conhecidas".
  success: faCircleCheck,
  gauge: faGaugeHigh,
  route: faRoute,
  calendarCheck: faCalendarCheck,
};
