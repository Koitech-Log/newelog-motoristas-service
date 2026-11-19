package br.com.newelog.motoristas.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.model.Viagem;
import br.com.newelog.motoristas.repository.ViagemRepository;
import jakarta.persistence.EntityManager;

/** Destino da viagem vindo do manifesto (CAR2a-1..9, 12 em regras-e-decisoes). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "GESTOR")
class DestinoViagemManifestoTest {

    @Autowired ManifestoCadastroService cadastro;
    @Autowired ViagemRepository viagens;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;

    private static ManifestoCadastroDTO linha(LocalDate data, double frete, String destino) {
        return new ManifestoCadastroDTO("M-DESTINO", data, "Motorista Destino", "52998224725",
                "ABC1D23", "Finalizado", frete, null, destino);
    }

    private static ManifestoCadastroDTO linha(String destino) {
        return linha(LocalDate.of(2026, 4, 10), 1000.00, destino);
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    private List<Viagem> todas() {
        return viagens.findAll().stream().sorted(Comparator.comparing(Viagem::getId)).toList();
    }

    @Test
    void gravaODestinoDaLinhaComAcento() {
        cadastro.cadastrar(linha("São Paulo"));
        flush();

        assertThat(todas()).singleElement().extracting(Viagem::getDestino).isEqualTo("São Paulo");
    }

    @Test
    void cadaViagemFicaComOProprioDestinoSemHerancaDaAnterior() {
        cadastro.cadastrar(linha(LocalDate.of(2026, 4, 10), 1000.00, "São Paulo"));
        cadastro.cadastrar(linha(LocalDate.of(2026, 4, 11), 800.00, "Campinas"));
        cadastro.cadastrar(linha(LocalDate.of(2026, 4, 12), 700.00, null));
        flush();

        assertThat(todas()).extracting(Viagem::getDestino)
                .containsExactly("São Paulo", "Campinas", "Não informado");
    }

    @Test
    void removeEspacosDasPontasEMantemListaDeCidades() {
        cadastro.cadastrar(linha("  São Paulo, Cotia  "));
        flush();

        assertThat(todas().get(0).getDestino()).isEqualTo("São Paulo, Cotia");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void destinoVazioViraNaoInformado(String vazio) {
        cadastro.cadastrar(linha(vazio));
        flush();

        assertThat(todas().get(0).getDestino()).isEqualTo("Não informado");
    }

    @Test
    void reimportacaoCorrigeNaoInformadoSemCriarViagem() {
        cadastro.cadastrar(linha(null));
        flush();
        Long id = todas().get(0).getId();

        cadastro.cadastrar(linha("São Paulo"));
        flush();

        assertThat(todas()).singleElement().satisfies(v -> {
            assertThat(v.getId()).isEqualTo(id);
            assertThat(v.getDestino()).isEqualTo("São Paulo");
        });
    }

    @Test
    void reimportacaoCorrigeDestinoDiferente() {
        cadastro.cadastrar(linha("Campinas"));
        flush();

        cadastro.cadastrar(linha("São Paulo"));
        flush();

        assertThat(todas()).singleElement().extracting(Viagem::getDestino).isEqualTo("São Paulo");
    }

    @Test
    void reimportacaoCorrigeDestinoEPedagioNaMesmaLinha() {
        cadastro.cadastrar(new ManifestoCadastroDTO("M-DESTINO", LocalDate.of(2026, 4, 10),
                "Motorista Destino", "52998224725", "ABC1D23", "Finalizado", 1000.00,
                new BigDecimal("0.00"), null));
        flush();

        cadastro.cadastrar(new ManifestoCadastroDTO("M-DESTINO", LocalDate.of(2026, 4, 10),
                "Motorista Destino", "52998224725", "ABC1D23", "Finalizado", 1000.00,
                new BigDecimal("165.87"), "São Paulo"));
        flush();

        assertThat(todas()).singleElement().satisfies(v -> {
            assertThat(v.getDestino()).isEqualTo("São Paulo");
            assertThat(v.getValorPedagio()).isEqualByComparingTo("165.87");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void reimportacaoSemDestinoMantemOGravado(String vazio) {
        cadastro.cadastrar(linha("São Paulo"));
        flush();

        cadastro.cadastrar(linha(vazio));
        cadastro.cadastrar(linha(null));
        flush();

        assertThat(todas()).singleElement().extracting(Viagem::getDestino).isEqualTo("São Paulo");
    }

    @Test
    void reimportacaoComVariasViagensCorrespondentesNaoAlteraNenhuma() {
        cadastro.cadastrar(linha("Campinas"));
        flush();
        Viagem primeira = todas().get(0);
        Viagem segunda = new Viagem();
        segunda.setMotorista(em.merge(primeira).getMotorista());
        segunda.setData(primeira.getData());
        segunda.setOrigem(primeira.getOrigem());
        segunda.setDestino("Não informado");
        segunda.setValorFrete(primeira.getValorFrete());
        em.persist(segunda);
        flush();

        cadastro.cadastrar(linha("São Paulo"));
        flush();

        assertThat(todas()).extracting(Viagem::getDestino)
                .containsExactly("Campinas", "Não informado");
    }

    @Test
    void reimportarOMesmoArquivoDuasVezesEIdempotente() {
        List<ManifestoCadastroDTO> arquivo = List.of(
                linha(LocalDate.of(2026, 4, 10), 1000.00, "São Paulo"),
                linha(LocalDate.of(2026, 4, 11), 800.00, null),
                linha(LocalDate.of(2026, 4, 12), 700.00, "Campinas"));

        arquivo.forEach(cadastro::cadastrar);
        flush();
        List<String> primeira = todas().stream()
                .map(v -> v.getId() + "|" + v.getDestino() + "|" + v.getValorPedagio()).toList();

        arquivo.forEach(cadastro::cadastrar);
        flush();
        List<String> segunda = todas().stream()
                .map(v -> v.getId() + "|" + v.getDestino() + "|" + v.getValorPedagio()).toList();

        assertThat(segunda).hasSize(3).isEqualTo(primeira);
    }

    private static String json(String trechoDestino) {
        return """
            {"codigoExterno":"M-DESTINO","data":"2026-04-10",
             "nomeMotorista":"Motorista Destino","cpfMotorista":"52998224725",
             "placaVeiculo":"ABC1D23","valorFrete":1000.00%s}
            """.formatted(trechoDestino);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"destino\":null"})
    void clienteAntigoSemDestinoContinuaFuncionando(String trecho) throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON).content(json(trecho)))
                .andExpect(status().isOk());
        flush();

        assertThat(todas()).singleElement().extracting(Viagem::getDestino).isEqualTo("Não informado");
    }

    @Test
    void cadastroAceitaDestinoCom120Caracteres() throws Exception {
        String destino = "A".repeat(120);
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(",\"destino\":\"" + destino + "\"")))
                .andExpect(status().isOk());
        flush();

        assertThat(todas().get(0).getDestino()).isEqualTo(destino);
    }

    @Test
    void cadastroRejeitaDestinoComMaisDe120CaracteresSemGravar() throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(",\"destino\":\"" + "A".repeat(121) + "\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("destino: destino deve ter no máximo 120 caracteres"));
        flush();

        assertThat(viagens.count()).isZero();
    }

    private static String jsonValidacao(String trechoDestino) {
        return """
            {"manifestoID":"M-DESTINO","data":"2026-04-10",
             "nomeMotorista":"Motorista Destino","cpfMotorista":"52998224725",
             "placaVeiculo":"ABC1D23","valorFrete":1000.00,
             "totalDespesas":0,"saldoAPagar":0%s}
            """.formatted(trechoDestino);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"destino\":null", ",\"destino\":\"São Paulo\"",
            ",\"destino\":\"   \""})
    void validacaoAceitaDestinoOpcional(String trecho) throws Exception {
        mvc.perform(post("/api/motoristas/validar-manifesto")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonValidacao(trecho)))
                .andExpect(status().isOk());
    }

    @Test
    void validacaoRejeitaDestinoComMaisDe120Caracteres() throws Exception {
        mvc.perform(post("/api/motoristas/validar-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValidacao(",\"destino\":\"" + "A".repeat(121) + "\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void espacosNasPontasNaoContamNoLimite() throws Exception {
        String destino = "A".repeat(120);
        mvc.perform(post("/api/motoristas/validar-manifesto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValidacao(",\"destino\":\"  " + destino + "  \"")))
                .andExpect(status().isOk());
    }
}
