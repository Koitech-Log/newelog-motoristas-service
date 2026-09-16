# motoristas-web

Front-end da tela de Motoristas — React + TypeScript + Vite, consumindo o
[`motoristas-service`](../motoristas-service) real. Demais telas do produto
(Home, Frota, Ranking, importação de manifesto) ainda não têm back-end pronto
e ficam fora desta primeira versão — ver documento "Adendo: Decomposição em
Microsserviços".

## Stack

- React 19.3 + TypeScript 7
- Vite 8 (dev server e build)
- FontAwesome (`@fortawesome/react-fontawesome`) para os ícones
- CSS Modules (sem framework de UI — a paleta e os componentes espelham o
  protótipo visual já validado com o parceiro), com animações de entrada e
  microinterações (cards escalonados, medidor radial que "enche" ao carregar,
  transições em foco/hover/clique). Tudo respeita `prefers-reduced-motion`.

## Rodando localmente

Pré-requisito: o `motoristas-service` rodando em `http://localhost:8080`
(ver README daquele repositório).

```bash
npm install
cp .env.example .env
npm run dev
```

Abre em `http://localhost:5173`.

## Build de produção

```bash
npm run build
npm run preview
```

## Estrutura

```
src/
├── api/          → cliente HTTP tipado (client.ts) e chamadas específicas (motoristas.ts)
├── types/        → tipos espelhando os DTOs Java do motoristas-service
├── hooks/        → useMotoristas (listagem + filtros), useMotoristaDetalhe (drawer)
├── components/   → DriverCard, DriverFilters, DriverDrawer, RadialGauge
└── pages/        → DriversPage (única página desta versão)
```

## Contrato com o back-end

Os tipos em `src/types/motorista.ts` espelham exatamente
`MotoristaResumoDTO`, `MotoristaDetalheDTO` e `ViagemResponseDTO` do
`motoristas-service`. Se o contrato do back-end mudar, este arquivo é o
primeiro lugar a atualizar.

Endpoints usados:

| Método | Rota | Uso neste front-end |
|---|---|---|
| GET | `/api/motoristas?status=&busca=` | Grade de cards da tela de Motoristas |
| GET | `/api/motoristas/{id}` | Conteúdo do drawer de detalhe |
| PATCH | `/api/motoristas/{id}/status` | Botões "Disponível" / "Em operação" no drawer |

## Pendências conhecidas

- Sem gateway ainda: a URL do back-end é configurada direto via
  `VITE_API_BASE_URL` (ver `.env.example`). Quando o `gateway-service`
  existir, só essa variável muda.
- Filtro por destino existe no back-end (`?destino=`) mas ainda não tem
  campo de UI aqui — a lista de destinos distintos precisaria vir de um
  endpoint próprio para popular um `<select>`, o que não existe ainda no
  `motoristas-service`.
- Paginação: o back-end retorna a lista completa (102 motoristas no seed);
  não há paginação real de servidor implementada neste front nem no back
  ainda — para esse volume a listagem completa é aceitável, mas não
  escalaria para uma frota muito maior sem paginação de verdade.
