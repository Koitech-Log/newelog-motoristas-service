# motoristas-service

Cadastro, consulta e disponibilidade dos motoristas agregados da frota Newelog.
Parte da arquitetura de microsserviços descrita no documento *"Adendo: Decomposição
em Microsserviços"*.

## Stack

- Java 21 · Spring Boot 3.5
- Spring Data JPA + PostgreSQL 16
- Spring Security (resource server, JWT HS256)
- Flyway (migrations versionadas)
- Maven (via wrapper `./mvnw`)

## Configuração

Variáveis de ambiente (modelo em `.env.example` — copie para `.env`, que não é versionado):

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_URL` | sim | URL JDBC, ex.: `jdbc:postgresql://localhost:5432/motoristas_db` |
| `DB_USERNAME` / `DB_PASSWORD` | sim | Credenciais do PostgreSQL |
| `JWT_SECRET` | sim | Mesmo segredo do `newelog-auth-service`, mínimo 32 caracteres. A aplicação não sobe sem ele |
| `PORT` | não | Porta HTTP (padrão `8080`) |
| `FRONTEND_URL` | não | Origem liberada no CORS (padrão `http://localhost:5173`) |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_CONNECTION_TIMEOUT` | não | Ajustes do pool Hikari |

## Rodando localmente

O Spring **não lê o `.env` sozinho**; carregue as variáveis no terminal antes de iniciar.

```bash
cp .env.example .env          # edite JWT_SECRET
docker compose up -d motoristas-db

# Linux/macOS/Git Bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

```powershell
# PowerShell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2; [Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim())
}
.\mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. As migrations do Flyway rodam
automaticamente na inicialização — `V1` cria o schema e `V2` popula a base
com os motoristas ativos em agosto/2026 (dados reais do parceiro, incluindo
CPF/CNPJ; sem PIS, data de nascimento ou endereço — ver seção "Dados").

## Rodando tudo em container

```bash
docker compose up --build
```

O `docker compose` lê o `.env` para substituir `JWT_SECRET`, `DB_USERNAME` e
`DB_PASSWORD`. Sem `JWT_SECRET` definido, o comando falha com mensagem explicativa.

## Autenticação e perfis

Todas as rotas, exceto `/actuator/health` e `/actuator/info`, exigem
`Authorization: Bearer <jwt>` emitido pelo `newelog-auth-service`:

- assinatura HS256 com `JWT_SECRET`;
- `iss = newelog-auth-service`;
- claim `perfil` = `GESTOR` ou `OPERADOR` (vira `ROLE_*`).

| Perfil | Acesso |
|---|---|
| `GESTOR` | Todas as rotas, inclusive cadastro manual (`POST /api/motoristas`) |
| `OPERADOR` | Todas as rotas, exceto `POST /api/motoristas` |

Sem token ou token inválido/expirado → `401`. Perfil sem permissão → `403`.

## Endpoints

| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/motoristas` | Gestor, Operador | Lista paginada. Aceita `?status=DISPONIVEL\|EM_OPERACAO`, `?destino=` (contém, case-insensitive), `?tipoVeiculo=`, `?busca=` (nome ou CPF, com ou sem pontuação), `?page=` (padrão 0, mínimo 0), `?size=` (padrão 12, entre 1 e 500) |
| GET | `/api/motoristas/{id}` | Gestor, Operador | Detalhe — CPF/CNPJ, telefone, rentabilidade e histórico de viagens |
| PATCH | `/api/motoristas/{id}/status` | Gestor, Operador | Atualiza a disponibilidade (`{"status": "DISPONIVEL"}`) |
| POST | `/api/motoristas` | Gestor | Cadastro manual. Corpo: `nome` (obrigatório), `codigoExterno`, `cpfCnpj`, `telefone`, `veiculoId`, `status`, `diasDisponiveis`, `diasOperacao`. Retorna `201`. `id`, `viagens` e `cadastroValidado` enviados pelo cliente são ignorados |
| POST | `/api/motoristas/validar-manifesto` | Gestor, Operador | Valida uma linha de manifesto (CPF, placa, regras de negócio). `200` se válida; `400` com a lista `erros` se não |
| POST | `/api/motoristas/cadastrar-de-manifesto` | Gestor, Operador | Efetiva o cadastro a partir de uma linha já validada (ver abaixo). Retorna o detalhe do motorista |
| GET | `/actuator/health` | público | Health check |

### Cadastro a partir do manifesto

`POST /api/motoristas/cadastrar-de-manifesto` é chamado pelo manifesto-service
depois de `validar-manifesto`:

- o motorista é localizado pelo **CPF/CNPJ** (upsert); se não existir, é criado
  com `cadastroValidado = false` (sinalizado para validação manual);
- nome, status e veículo são sempre atualizados com o dado mais recente;
- o veículo é resolvido pela **placa**, normalizada (maiúsculas, sem hífen);
  se não existir, é criado. Formatos aceitos: `ABC1234`, `ABC1D23`, `ABC-1234`, `ABC-1D23`;
- o status do manifesto vira `EM_OPERACAO` se contiver "operação", "viagem" ou
  "andamento"; qualquer outro valor vira `DISPONIVEL`;
- uma viagem é considerada duplicada (e não é recriada) quando o motorista já
  tem outra com a **mesma data e o mesmo valor de frete**.

### Erros

Corpo padrão: `{"timestamp": "...", "mensagem": "..."}` (mais `erros` nas falhas de
regra de negócio). Códigos usados: `400` (dados inválidos ou regra de negócio),
`401`, `403`, `404` (motorista não encontrado) e `409` (violação de unicidade,
como CPF/CNPJ, código externo ou placa já cadastrados).

## Testes

```bash
./mvnw clean verify
```

Os testes usam H2 em memória (perfil `test`) — não é necessário ter o
PostgreSQL rodando para executar `./mvnw test`. Só a migration `V1` (schema)
é aplicada nesse perfil; o seed de dados reais (`V2`) usa `SELECT setval()`,
função específica do PostgreSQL não suportada pelo H2. A autenticação JWT e os
perfis são testados ponta a ponta em `SegurancaJwtTest`.

## Dados

O seed inicial (`V2__seed_dados_iniciais.sql`) reflete os manifestos reais
de agosto/2026 fornecidos pelo parceiro. Por pedido explícito do parceiro
(documento "Respostas do Parceiro e Dados Reais", pergunta 9), o cadastro
armazena CPF/CNPJ do motorista — dado pessoal sujeito à LGPD; mantenha o
repositório privado. Telefone também está modelado, mas fica `NULL` no seed
atual — não está disponível na fonte de dados processada até agora. PIS, data
de nascimento e endereço continuam fora do modelo, por não terem sido pedidos
pelo parceiro e não agregarem valor à operação do dashboard.

Quilometragem por viagem também não é um campo modelado: os manifestos reais
analisados não trazem essa informação de forma confiável (ver documento
"Respostas do Parceiro e Dados Reais", seção 3) — a regra de negócio do
parceiro proíbe expor números que o sistema não consegue garantir corretos.
Pela mesma regra, a rentabilidade exposta em `/api/motoristas/{id}`
(`rentabilidadeTotal = valorFreteTotal - valorPedagioTotal`) é parcial: o
pedágio é hoje o único custo confirmado por motorista.

## Pendências conhecidas (ver documentos de projeto)

- Granularidade Manifesto → Viagem → Entrega ainda não implementada (hoje 1
  viagem = 1 linha do manifesto de origem).
- A viagem não guarda identificador de origem do manifesto; a deduplicação por
  data + frete pode fundir duas viagens distintas do mesmo dia e valor.
- `cpf_cnpj` ainda não tem índice único no banco (a unicidade é verificada pela
  aplicação).
