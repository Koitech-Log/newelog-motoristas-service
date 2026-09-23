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
| GET | `/api/motoristas` | Lista paginada. Aceita `?status=DISPONIVEL\|EM_OPERACAO`, `?destino=` (contém, case-insensitive), `?busca=` (nome ou CPF, com ou sem pontuação), `?page=` (padrão 0), `?size=` (padrão 12) |
| GET | `/api/motoristas/{id}` | Detalhe de um motorista — CPF/CNPJ, telefone, rentabilidade e histórico de viagens |
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
