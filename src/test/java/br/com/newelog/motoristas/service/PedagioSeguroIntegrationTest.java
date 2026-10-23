package br.com.newelog.motoristas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.model.Viagem;
import br.com.newelog.motoristas.repository.ViagemRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "GESTOR")
class PedagioSeguroIntegrationTest {
    @Autowired ManifestoCadastroService cadastro;
    @Autowired ViagemRepository viagens;
    @Autowired RentabilidadeAnaliseService dashboard;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;

    private ManifestoCadastroDTO dados(String pedagio) {
        return new ManifestoCadastroDTO("TESTE-PEDAGIO", LocalDate.of(2026, 7, 1),
            "Motorista Teste", "52998224725", "ABC1D23", "Finalizado", 2129.70,
            pedagio == null ? null : new BigDecimal(pedagio));
    }
    private void flush() { em.flush(); em.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "0.01", "165.87", "9999999999.99"})
    void persisteMoedaExataECalculaDashboard(String pedagio) {
        cadastro.cadastrar(dados(pedagio));
        flush();
        assertThat(viagens.count()).isEqualTo(1);
        assertThat(viagens.findAll().get(0).getValorPedagio()).isEqualByComparingTo(pedagio);
        var geral = dashboard.buscar(2026, 7).geral();
        assertThat(geral.custoTotal()).isEqualByComparingTo(pedagio);
        assertThat(geral.rentabilidadeTotal()).isEqualByComparingTo(new BigDecimal("2129.70").subtract(new BigDecimal(pedagio)));
        assertThat(geral.rentabilidadeMedia()).isEqualByComparingTo(geral.rentabilidadeTotal());
        assertThat(geral.quantidadeViagens()).isEqualTo(1);
        assertThat(dashboard.buscar(2026, 7).porMotorista().get(0).custoTotal()).isEqualByComparingTo(pedagio);
        assertThat(dashboard.buscar(2026, 7).porVeiculo().get(0).custoTotal()).isEqualByComparingTo(pedagio);
        assertThat(dashboard.buscar(2026, 8).geral().quantidadeViagens()).isZero();
    }

    @Test
    void corrigeZeroEReimportaSemDuplicar() {
        cadastro.cadastrar(dados("0.00"));
        flush();
        Long id = viagens.findAll().get(0).getId();
        cadastro.cadastrar(dados("165.87"));
        flush();
        cadastro.cadastrar(dados("165.87"));
        flush();
        assertThat(viagens.count()).isEqualTo(1);
        assertThat(viagens.findAll().get(0).getId()).isEqualTo(id);
        assertThat(viagens.findAll().get(0).getValorPedagio()).isEqualByComparingTo("165.87");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0.00", "0.01", "165.87", "200.00"})
    void nuncaSobrescrevePedagioJaPreenchido(String recebido) {
        cadastro.cadastrar(dados("165.87"));
        flush();
        cadastro.cadastrar(dados(recebido));
        flush();
        assertThat(viagens.count()).isEqualTo(1);
        assertThat(viagens.findAll().get(0).getValorPedagio()).isEqualByComparingTo("165.87");
    }

    @Test
    void naoEscolheEntreVariasViagensCorrespondentes() {
        cadastro.cadastrar(dados("0.00"));
        flush();
        Viagem primeira = viagens.findAll().get(0);
        Viagem segunda = new Viagem();
        segunda.setMotorista(primeira.getMotorista());
        segunda.setData(primeira.getData());
        segunda.setOrigem(primeira.getOrigem());
        segunda.setDestino(primeira.getDestino());
        segunda.setValorFrete(primeira.getValorFrete());
        segunda.setValorPedagio(new BigDecimal("84.40"));
        em.persist(segunda);
        flush();
        cadastro.cadastrar(dados("200.00"));
        flush();
        assertThat(viagens.count()).isEqualTo(2);
        assertThat(viagens.findAll().stream().map(Viagem::getValorPedagio).toList())
            .containsExactlyInAnyOrder(new BigDecimal("0.00"), new BigDecimal("84.40"));
    }

    @Test
    void registrosDistintosContinuamSeparados() {
        cadastro.cadastrar(dados("10.00"));
        cadastro.cadastrar(new ManifestoCadastroDTO("OUTRO", LocalDate.of(2026, 7, 2),
            "Motorista Teste", "52998224725", "ABC1D23", "Finalizado", 2129.70, new BigDecimal("20.00")));
        cadastro.cadastrar(new ManifestoCadastroDTO("OUTRO", LocalDate.of(2026, 7, 1),
            "Motorista Teste", "52998224725", "ABC1D23", "Finalizado", 1000.00, new BigDecimal("30.00")));
        cadastro.cadastrar(new ManifestoCadastroDTO("OUTRO-MOTORISTA", LocalDate.of(2026, 7, 1),
            "Segundo Motorista", "11144477735", "DEF1G23", "Finalizado", 2129.70, new BigDecimal("40.00")));
        flush();
        assertThat(viagens.count()).isEqualTo(4);
        assertThat(dashboard.buscar(2026, 7).geral().custoTotal()).isEqualByComparingTo("100.00");
    }

    private String json(String trechoPedagio) {
        return """
            {"codigoExterno":"TESTE-PEDAGIO","data":"2026-07-01",
             "nomeMotorista":"Motorista Teste","cpfMotorista":"52998224725",
             "placaVeiculo":"ABC1D23","valorFrete":2129.70%s}
            """.formatted(trechoPedagio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"valorPedagio\":null"})
    void clienteAntigoOuNuloContinuaFuncionando(String trecho) throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                .contentType(MediaType.APPLICATION_JSON).content(json(trecho)))
            .andExpect(status().isOk());
        flush();
        assertThat(viagens.count()).isEqualTo(1);
        assertThat(viagens.findAll().get(0).getValorPedagio()).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.001", "10000000000", "\"abc\"", "\"NaN\""})
    void jsonInvalidoNaoGravaNada(String valor) throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                .contentType(MediaType.APPLICATION_JSON).content(json(",\"valorPedagio\":" + valor)))
            .andExpect(status().isBadRequest());
        flush();
        assertThat(viagens.count()).isZero();
    }

    @Test
    void endpointExibeRentabilidadeCorreta() throws Exception {
        mvc.perform(post("/api/motoristas/cadastrar-de-manifesto")
                .contentType(MediaType.APPLICATION_JSON).content(json(",\"valorPedagio\":165.87")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/rentabilidade-media").param("ano", "2026").param("mes", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.geral.custoTotal").value(165.87))
            .andExpect(jsonPath("$.geral.rentabilidadeMedia").value(1963.83));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void permissaoDoFinanceiroContinuaBloqueadaParaOperador() throws Exception {
        mvc.perform(get("/api/dashboard/rentabilidade-media").param("ano", "2026").param("mes", "7"))
            .andExpect(status().isForbidden());
    }
}
