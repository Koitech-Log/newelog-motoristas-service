package br.com.newelog.motoristas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.model.Viagem;
import br.com.newelog.motoristas.repository.ViagemRepository;
import jakarta.persistence.EntityManager;

/** Identificação da viagem pelo número do manifesto (P2, opção B). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "GESTOR")
class NumeroManifestoViagemTest {

    private static final LocalDate DATA = LocalDate.of(2026, 4, 10);

    @Autowired ManifestoCadastroService cadastro;
    @Autowired ViagemRepository viagens;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    private static ManifestoCadastroDTO linha(String numero, String destino, String pedagio) {
        return new ManifestoCadastroDTO("M-" + numero, DATA, "Motorista Numero", "52998224725",
                "ABC1D23", "Finalizado", 277.00, pedagio == null ? null : new BigDecimal(pedagio),
                destino, numero);
    }

    private static ManifestoCadastroDTO linha(String numero, String destino) {
        return linha(numero, destino, null);
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    private List<Viagem> todas() {
        return viagens.findAll().stream().sorted(Comparator.comparing(Viagem::getId)).toList();
    }

    /** Viagem gravada antes da V3: sem número, como a base atual. */
    private Long viagemAntigaSemNumero(String destino, String pedagio) {
        cadastro.cadastrar(new ManifestoCadastroDTO("ANTIGA", DATA, "Motorista Numero", "52998224725",
                "ABC1D23", "Finalizado", 277.00, new BigDecimal(pedagio), destino));
        flush();
        return todas().get(0).getId();
    }

    @Test
    void doisManifestosComMesmaDataEFreteViramDuasViagens() {
        cadastro.cadastrar(linha("17156", "São Paulo"));
        cadastro.cadastrar(linha("17180", "Campinas"));
        flush();

        assertThat(todas()).extracting(Viagem::getNumeroManifesto, Viagem::getDestino)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("17156", "São Paulo"),
                        org.assertj.core.groups.Tuple.tuple("17180", "Campinas"));
    }

    @Test
    void reimportarOMesmoArquivoNaoDuplicaEEIdempotente() {
        List<ManifestoCadastroDTO> arquivo = List.of(
                linha("17156", "São Paulo", "10.00"),
                linha("17180", "Campinas", "0.00"),
                linha("17198", null, null));

        arquivo.forEach(cadastro::cadastrar);
        flush();
        List<String> primeira = todas().stream().map(NumeroManifestoViagemTest::resumo).toList();

        arquivo.forEach(cadastro::cadastrar);
        flush();
        List<String> segunda = todas().stream().map(NumeroManifestoViagemTest::resumo).toList();

        assertThat(segunda).hasSize(3).isEqualTo(primeira);
    }

    @Test
    void reimportacaoPeloNumeroCorrigeDestinoEPedagioDaPropriaViagem() {
        cadastro.cadastrar(linha("17156", null, "0.00"));
        cadastro.cadastrar(linha("17180", "Campinas", "5.00"));
        flush();

        cadastro.cadastrar(linha("17156", "São Paulo", "12.34"));
        flush();

        assertThat(todas()).extracting(Viagem::getNumeroManifesto, Viagem::getDestino, v -> v.getValorPedagio().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("17156", "São Paulo", "12.34"),
                        org.assertj.core.groups.Tuple.tuple("17180", "Campinas", "5.00"));
    }

    @Test
    void baseAntigaSemNumeroGanhaONumeroSemDuplicar() {
        Long id = viagemAntigaSemNumero("Não informado", "0.00");

        cadastro.cadastrar(linha("17156", "São Paulo", "8.50"));
        flush();

        assertThat(todas()).singleElement().satisfies(v -> {
            assertThat(v.getId()).isEqualTo(id);
            assertThat(v.getNumeroManifesto()).isEqualTo("17156");
            assertThat(v.getDestino()).isEqualTo("São Paulo");
            assertThat(v.getValorPedagio()).isEqualByComparingTo("8.50");
        });
    }

    @Test
    void segundoManifestoDaChaveAntigaViraViagemNovaDepoisQueAAntigaGanhouNumero() {
        // Era o caso descartado pela deduplicação antiga (P2): a base tinha 1 viagem para 2 manifestos
        Long id = viagemAntigaSemNumero("São Paulo", "0.00");

        cadastro.cadastrar(linha("17156", "São Paulo"));
        cadastro.cadastrar(linha("17180", "Campinas"));
        flush();

        assertThat(todas()).extracting(Viagem::getId, Viagem::getNumeroManifesto, Viagem::getDestino)
                .first().isEqualTo(org.assertj.core.groups.Tuple.tuple(id, "17156", "São Paulo"));
        assertThat(todas()).hasSize(2).last()
                .extracting(Viagem::getNumeroManifesto, Viagem::getDestino)
                .containsExactly("17180", "Campinas");
    }

    @Test
    void viagemComOutroNumeroNaoECasadaPeloCriterioAntigo() {
        cadastro.cadastrar(linha("17156", "São Paulo"));
        flush();

        cadastro.cadastrar(linha("17180", "Campinas"));
        flush();

        assertThat(todas()).extracting(Viagem::getNumeroManifesto).containsExactly("17156", "17180");
        assertThat(todas().get(0).getDestino()).isEqualTo("São Paulo");
    }

    @Test
    void variasViagensAntigasSemNumeroRecebemOsNumerosEmOrdemSemDuplicar() {
        Long primeira = viagemAntigaSemNumero("Campinas", "0.00");
        Viagem copia = new Viagem();
        copia.setMotorista(em.find(Viagem.class, primeira).getMotorista());
        copia.setData(DATA);
        copia.setOrigem("São José dos Campos");
        copia.setDestino("Campinas");
        copia.setValorFrete(new BigDecimal("277.00"));
        em.persist(copia);
        flush();

        cadastro.cadastrar(linha("17156", "São Paulo"));
        cadastro.cadastrar(linha("17180", "Guarulhos"));
        cadastro.cadastrar(linha("17156", "São Paulo"));
        flush();

        assertThat(todas()).extracting(Viagem::getNumeroManifesto, Viagem::getDestino)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("17156", "São Paulo"),
                        org.assertj.core.groups.Tuple.tuple("17180", "Guarulhos"));
    }

    @Test
    void semNumeroMantemOCriterioAntigo() {
        cadastro.cadastrar(new ManifestoCadastroDTO("A", DATA, "Motorista Numero", "52998224725",
                "ABC1D23", "Finalizado", 277.00, null, "São Paulo"));
        cadastro.cadastrar(new ManifestoCadastroDTO("B", DATA, "Motorista Numero", "52998224725",
                "ABC1D23", "Finalizado", 277.00, null, "Campinas"));
        flush();

        assertThat(todas()).singleElement().satisfies(v -> {
            assertThat(v.getNumeroManifesto()).isNull();
            assertThat(v.getDestino()).isEqualTo("Campinas");
        });
    }

    @Test
    void numeroComEspacosEGravadoSemEspacosEVazioValeComoSemNumero() {
        cadastro.cadastrar(linha("  17156  ", "São Paulo"));
        cadastro.cadastrar(linha("   ", "Campinas"));
        flush();

        assertThat(todas()).singleElement().satisfies(v -> {
            assertThat(v.getNumeroManifesto()).isEqualTo("17156");
            assertThat(v.getDestino()).isEqualTo("Campinas");
        });
    }

    @Test
    void bancoRejeitaNumeroDeManifestoRepetidoEAceitaVariosNulos() {
        Long id = viagemAntigaSemNumero("Campinas", "0.00");
        Long motoristaId = jdbc.queryForObject("SELECT motorista_id FROM viagem WHERE id = ?", Long.class, id);
        String insert = "INSERT INTO viagem (motorista_id, data, destino, valor_frete, valor_pedagio, numero_manifesto) "
                + "VALUES (?, DATE '2026-04-11', 'X', 1, 0, ?)";

        jdbc.update(insert, motoristaId, null);
        jdbc.update(insert, motoristaId, "99999");

        assertThatThrownBy(() -> jdbc.update(insert, motoristaId, "99999"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String json(String trechoNumero) {
        return """
            {"codigoExterno":"M-1","data":"2026-04-10",
             "nomeMotorista":"Motorista Numero","cpfMotorista":"52998224725",
             "placaVeiculo":"ABC1D23","valorFrete":277.00%s}
            """.formatted(trechoNumero);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"numeroManifesto\":null", ",\"numeroManifesto\":\"17156\""})
    void endpointAceitaNumeroOpcional(String trecho) throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON).content(json(trecho)))
                .andExpect(status().isOk());
        flush();

        assertThat(todas()).hasSize(1);
    }

    @Test
    void endpointRejeitaNumeroComMaisDe40Caracteres() throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(",\"numeroManifesto\":\"" + "9".repeat(41) + "\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("numeroManifesto: numeroManifesto deve ter no máximo 40 caracteres"));
        flush();

        assertThat(viagens.count()).isZero();
    }

    @Test
    void validacaoAceitaNumeroOpcional() throws Exception {
        mvc.perform(post("/api/motoristas/validar-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"manifestoID":"17156","data":"2026-04-10","nomeMotorista":"X",
                             "cpfMotorista":"52998224725","placaVeiculo":"ABC1D23","valorFrete":1,
                             "totalDespesas":0,"saldoAPagar":0,"numeroManifesto":"17156"}
                            """))
                .andExpect(status().isOk());
    }

    // D20 / CAR3-P2-7: mesmo número com motorista, data ou frete diferentes

    private void naoMudaAViagem(String antes) {
        em.clear();
        assertThat(todas()).singleElement().extracting(NumeroManifestoViagemTest::resumo).isEqualTo(antes);
    }

    @Test
    void mesmoNumeroComDataDiferenteERejeitadoSemAlterarAViagem() {
        cadastro.cadastrar(linha("17156", "São Paulo", "10.00"));
        flush();
        String antes = resumo(todas().get(0));

        assertThatThrownBy(() -> cadastro.cadastrar(new ManifestoCadastroDTO("M-17156", DATA.plusDays(1),
                "Motorista Numero", "52998224725", "ABC1D23", "Finalizado", 277.00, null, "Campinas", "17156")))
                .isInstanceOf(ValidacaoException.class)
                .extracting("erros").asList().singleElement().asString()
                .isEqualTo("Manifesto 17156 já importado com dados diferentes: data (gravada 2026-04-10, recebida 2026-04-11)");
        naoMudaAViagem(antes);
    }

    @Test
    void mesmoNumeroComFreteDiferenteERejeitado() {
        cadastro.cadastrar(linha("17156", "São Paulo"));
        flush();
        String antes = resumo(todas().get(0));

        assertThatThrownBy(() -> cadastro.cadastrar(new ManifestoCadastroDTO("M-17156", DATA,
                "Motorista Numero", "52998224725", "ABC1D23", "Finalizado", 300.00, null, "Campinas", "17156")))
                .isInstanceOf(ValidacaoException.class)
                .extracting("erros").asList().singleElement().asString()
                .contains("Manifesto 17156", "valorFrete (gravado 277.00, recebido 300.00)");
        naoMudaAViagem(antes);
    }

    @Test
    void mesmoNumeroEmOutroMotoristaERejeitadoCitandoTodosOsCampos() throws Exception {
        cadastro.cadastrar(linha("17156", "São Paulo"));
        flush();
        String antes = resumo(todas().get(0));

        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"codigoExterno":"M-17156","data":"2026-04-12","nomeMotorista":"Outro Motorista",
                             "cpfMotorista":"11144477735","placaVeiculo":"DEF1G23","valorFrete":500.00,
                             "destino":"Campinas","numeroManifesto":"17156"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0]").value(
                        "Manifesto 17156 já importado com dados diferentes: "
                        + "motorista (gravado Motorista Numero, recebido Outro Motorista), "
                        + "data (gravada 2026-04-10, recebida 2026-04-12), "
                        + "valorFrete (gravado 277.00, recebido 500.00)"));
        naoMudaAViagem(antes);
    }

    private static String resumo(Viagem v) {
        return v.getId() + "|" + v.getNumeroManifesto() + "|" + v.getDestino() + "|" + v.getValorPedagio();
    }
}
