-- Identifica a viagem pelo número do manifesto (coluna "Manifesto" do CSV).
-- Aditiva: viagens antigas ficam com NULL e ganham o número na primeira
-- reimportação (ver ManifestoCadastroService).
ALTER TABLE viagem ADD COLUMN numero_manifesto VARCHAR(40);

-- Único entre as viagens que têm número. Um índice único comum já permite
-- vários NULL no PostgreSQL (NULLS DISTINCT é o padrão), então tem o mesmo
-- efeito de um índice parcial WHERE numero_manifesto IS NOT NULL e também
-- roda no H2 dos testes. Protege contra duas importações simultâneas do
-- mesmo manifesto.
CREATE UNIQUE INDEX uk_viagem_numero_manifesto ON viagem (numero_manifesto);
