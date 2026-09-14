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

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/motoristas` | Lista motoristas. Aceita `?status=DISPONIVEL\|EM_OPERACAO`, `?destino=`, `?busca=` |
| GET | `/api/motoristas/{id}` | Detalhe de um motorista, com histórico de viagens |
| PATCH | `/api/motoristas/{id}/status` | Atualiza a disponibilidade (`{"status": "DISPONIVEL"}`) |
| GET | `/actuator/health` | Health check |

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
de agosto/2026 fornecidos pelo parceiro, processados para remover qualquer
dado pessoal sensível. Campos como CPF, PIS, data de nascimento e endereço
do motorista **não têm equivalente no modelo de dados deste serviço** — essa
foi uma decisão de produto, não uma omissão.

Quilometragem por viagem também não é um campo modelado: os manifestos reais
analisados não trazem essa informação de forma confiável (ver documento
"Respostas do Parceiro e Dados Reais", seção 3) — a regra de negócio do
parceiro proíbe expor números que o sistema não consegue garantir corretos.

## Pendências conhecidas (ver documentos de projeto)

- Granularidade Manifesto → Viagem → Entrega ainda não implementada (hoje 1
  viagem = 1 linha do manifesto de origem).
- Fluxo de "motorista novo sinalizado para validação" (regra do parceiro)
  existe no protótipo front-end mas ainda não neste serviço — o campo
  `cadastroValidado` já existe no modelo para suportar essa regra quando o
  endpoint de importação de manifesto for implementado.
