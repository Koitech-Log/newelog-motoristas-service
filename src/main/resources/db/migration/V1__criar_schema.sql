-- V1__criar_schema.sql
-- Modelo de dados do motoristas-service.
-- Segue a modelagem definida no documento "Arquitetura Técnica" (seção 2.1),
-- com o ajuste de granularidade Manifesto->Viagem->Entrega deixado como
-- evolução futura (ver comentário na entidade Viagem).
--
-- motorista.cpf_cnpj e motorista.telefone: armazenados por pedido explícito
-- do parceiro (documento "Respostas do Parceiro e Dados Reais", pergunta 9).
-- telefone não está disponível na fonte de dados processada até agora e é
-- populado como NULL no seed (V2).

CREATE TABLE veiculo_tipo (
    id     BIGSERIAL PRIMARY KEY,
    nome   VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE veiculo (
    id                BIGSERIAL PRIMARY KEY,
    placa             VARCHAR(10) NOT NULL UNIQUE,
    marca             VARCHAR(60),
    veiculo_tipo_id   BIGINT REFERENCES veiculo_tipo(id)
);

CREATE TABLE motorista (
    id                  BIGSERIAL PRIMARY KEY,
    codigo_externo      VARCHAR(120) UNIQUE,
    nome                VARCHAR(150) NOT NULL,
    cpf_cnpj            VARCHAR(20),
    telefone            VARCHAR(20),
    veiculo_id          BIGINT REFERENCES veiculo(id),
    status              VARCHAR(20) NOT NULL,
    dias_disponiveis    INTEGER NOT NULL DEFAULT 0,
    dias_operacao       INTEGER NOT NULL DEFAULT 0,
    cadastro_validado   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE viagem (
    id              BIGSERIAL PRIMARY KEY,
    motorista_id    BIGINT NOT NULL REFERENCES motorista(id) ON DELETE CASCADE,
    data            DATE NOT NULL,
    origem          VARCHAR(120),
    destino         VARCHAR(120) NOT NULL,
    valor_frete     NUMERIC(12,2) NOT NULL DEFAULT 0,
    valor_pedagio   NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_motorista_status ON motorista(status);
CREATE INDEX idx_viagem_motorista ON viagem(motorista_id);
CREATE INDEX idx_viagem_destino ON viagem(destino);
