// Import individual de cada ícone.
// Evita importar o pacote inteiro e mantém o bundle enxuto.

import {
  // ==================== SIDEBAR / HEADER ====================
  faBars,
  faGaugeHigh,
  faTruck,
  faTruckFast,
  faIdCard,
  faRankingStar,
  faFileImport,
  faFileExport,
  faUserTie,
  faUserGear,

  // ==================== NAVEGAÇÃO / OUTRAS TELAS ====================
  faRoute,
  faCalendarCheck,
  faUserPlus,
  faInbox,

  // ==================== AÇÕES ====================
  faMagnifyingGlass,
  faChevronRight,
  faXmark,
  faArrowRotateRight,

  // ==================== STATUS ====================
  faCircleCheck,
  faCircleExclamation,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";

export const icons = {
  // ==================== SIDEBAR / HEADER ====================
  bars: faBars,
  gauge: faGaugeHigh,
  truck: faTruck,
  truckFast: faTruckFast,
  idCard: faIdCard,
  rankingStar: faRankingStar,
  fileImport: faFileImport,
  fileExport: faFileExport,
  userTie: faUserTie,
  userGear: faUserGear,

  // ==================== NAVEGAÇÃO / OUTRAS TELAS ====================
  route: faRoute,
  calendarCheck: faCalendarCheck,
  userPlus: faUserPlus,
  inbox: faInbox,

  // ==================== AÇÕES ====================
  search: faMagnifyingGlass,
  chevronRight: faChevronRight,
  close: faXmark,
  retry: faArrowRotateRight,

  // ==================== STATUS ====================
  success: faCircleCheck,
  error: faCircleExclamation,
  spinner: faSpinner,
};