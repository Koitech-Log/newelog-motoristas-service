# motoristas-service

Cadastro, consulta e disponibilidade dos motoristas agregados da frota Newelog.
Parte da arquitetura de microsserviços descrita no documento *"Adendo: Decomposição
em Microsserviços"*.

## Stack

- Java 21 · Spring Boot 3.4
- Spring Data JPA + PostgreSQL 16
- Flyway (migrations versionadas)
- Maven (via wrapper `./mvnw`)

## Rodando localmente

```bash
docker compose up -d motoristas-db

cp .env.example .env
# edite o .env: preencha DB_PASSWORD e JWT_SECRET (igual ao do auth-service)

set -a; source .env; set +a

./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. As migrations do Flyway rodam
automaticamente na inicialização — `V1` cria o schema, `V2` popula a base
com os motoristas ativos em agosto/2026 (dados reais do parceiro, sem CPF,
PIS, data de nascimento ou endereço — ver seção "Dados" abaixo).

## Rodando tudo em container

```bash
docker compose up --build
```

## Autenticação

As rotas de `/api/**` exigem o header `Authorization: Bearer <token>`, com o JWT emitido pelo [`newelog-auth-service`](https://github.com/Koitech-Log/newelog-auth-service). O serviço valida a assinatura (HS256) com o `JWT_SECRET`, que precisa ser o mesmo do auth-service. Sem token válido, a resposta é `401`. Apenas `/actuator/health` é público.

## Variáveis de ambiente

Copie `.env.example` para `.env` (o `.env` não é versionado).

| Variável                | Obrigatória | Padrão                  | Descrição                                       |
| ----------------------- | ----------- | ----------------------- | ----------------------------------------------- |
| `PORT`                  | não         | `8080`                  | Porta do serviço                                |
| `FRONTEND_URL`          | não         | `http://localhost:5173` | Origem do front-end (CORS)                      |
| `JWT_SECRET`            | **sim**     | —                       | Mín. 32 caracteres. Igual ao do `auth-service`  |
| `DB_URL`                | **sim**     | —                       | URL JDBC do PostgreSQL                          |
| `DB_USERNAME` / `DB_PASSWORD` | **sim** | —                       | Credenciais do banco                            |
| `DB_POOL_MAX_SIZE`      | não         | `10`                    | Máximo de conexões do pool                      |
| `DB_POOL_MIN_IDLE`      | não         | `2`                     | Mínimo de conexões ociosas                      |
| `DB_CONNECTION_TIMEOUT` | não         | `30000`                 | Timeout de conexão (ms)                         |

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| GET | `/api/motoristas` | Lista paginada. Aceita ... | Autenticado |
| GET | `/api/motoristas/{id}` | Detalhe de um motorista ... | Autenticado |
| PATCH | `/api/motoristas/{id}/status` | Atualiza a disponibilidade ... | Autenticado |
| GET | `/actuator/health` | Health check | Público |

## Testes

```bash
./mvnw clean verify
```

Os testes usam H2 em memória (perfil `test`) — não é necessário ter o
PostgreSQL rodando para executar `./mvnw test`. Só a migration `V1` (schema)
é aplicada nesse perfil; o seed de dados reais (`V2`) usa `SELECT setval()`,
função específica do PostgreSQL não suportada pelo H2.

## Dados

O seed inicial (`V2__seed_dados_iniciais.sql`) reflete os manifestos reais
de agosto/2026 fornecidos pelo parceiro. Por pedido explícito do parceiro
(documento "Respostas do Parceiro e Dados Reais", pergunta 9), o cadastro
armazena CPF/CNPJ do motorista. Telefone também está modelado, mas fica
`NULL` no seed atual — não está disponível na fonte de dados processada até
agora. PIS, data de nascimento e endereço continuam fora do modelo, por não
terem sido pedidos pelo parceiro e não agregarem valor à operação do
dashboard.

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
- Fluxo de "motorista novo sinalizado para validação" (regra do parceiro)
  existe no protótipo front-end mas ainda não neste serviço — o campo
  `cadastroValidado` já existe no modelo para suportar essa regra quando o
  endpoint de importação de manifesto for implementado.
