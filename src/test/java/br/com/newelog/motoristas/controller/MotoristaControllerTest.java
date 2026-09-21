package br.com.newelog.motoristas.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.model.VeiculoTipo;
import br.com.newelog.motoristas.model.Viagem;
import br.com.newelog.motoristas.repository.MotoristaRepository;
import br.com.newelog.motoristas.repository.VeiculoRepository;
import br.com.newelog.motoristas.repository.VeiculoTipoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MotoristaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private VeiculoTipoRepository veiculoTipoRepository;

    private Long motoristaId;

    /**
     * Popula um motorista mínimo antes de cada teste — a V2 (seed real) não
     * roda no perfil de teste, ver application-test.properties.
     */
    @BeforeEach
    void setUp() {
        VeiculoTipo tipo = veiculoTipoRepository.save(criarTipo("VUC"));
        Veiculo veiculo = veiculoRepository.save(criarVeiculo("TST1A23", "Fiat", tipo));

        Motorista motorista = new Motorista();
        motorista.setCodigoExterno("motorista-teste");
        motorista.setNome("Motorista de Teste");
        motorista.setCpfCnpj("123.456.789-00");
        motorista.setTelefone("(12) 99999-0000");
        motorista.setVeiculo(veiculo);
        motorista.setStatus(StatusMotorista.DISPONIVEL);
        motorista.setDiasDisponiveis(5);
        motorista.setDiasOperacao(15);
        motorista.setCadastroValidado(true);

        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        viagem.setData(java.time.LocalDate.of(2026, 8, 1));
        viagem.setOrigem("São José dos Campos");
        viagem.setDestino("São Paulo");
        viagem.setValorFrete(new java.math.BigDecimal("1500.00"));
        viagem.setValorPedagio(new java.math.BigDecimal("120.00"));
        motorista.getViagens().add(viagem);

        motoristaId = motoristaRepository.save(motorista).getId();
    }

    @Test
    void deveRetornarCpfTelefoneERentabilidadeNoDetalhe() throws Exception {
        mockMvc.perform(get("/api/motoristas/{id}", motoristaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpfCnpj", is("123.456.789-00")))
                .andExpect(jsonPath("$.telefone", is("(12) 99999-0000")))
                .andExpect(jsonPath("$.rentabilidadeTotal", is(0)));
    }

    @Test
    void deveListarMotoristasCadastrados() throws Exception {
        mockMvc.perform(get("/api/motoristas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].nome", is("Motorista de Teste")))
                .andExpect(jsonPath("$.totalElementos", is(1)))
                .andExpect(jsonPath("$.paginaAtual", is(0)));
    }

    @Test
    void deveFiltrarPorStatus() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("status", "DISPONIVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].status", is("DISPONIVEL")));
    }

    @Test
    void deveEncontrarPorCpfComPontuacao() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("busca", "123.456.789-00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));
    }

    @Test
    void deveEncontrarPorCpfSemPontuacao() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("busca", "12345678900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));
    }

    @Test
    void deveFiltrarPorDestinoParcialSemDiferenciarCaixa() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("destino", "paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));
    }

    @Test
    void naoDeveEncontrarDestinoInexistente() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("destino", "Manaus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(0)));
    }

    @Test
    void devePaginarComTamanhoPersonalizado() throws Exception {
        mockMvc.perform(get("/api/motoristas").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanhoPagina", is(1)))
                .andExpect(jsonPath("$.conteudo.length()", is(1)));
    }

    @Test
    void deveRetornar404ParaMotoristaInexistente() throws Exception {
        mockMvc.perform(get("/api/motoristas/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarStatusDoMotorista() throws Exception {
        mockMvc.perform(patch("/api/motoristas/{id}/status", motoristaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_OPERACAO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EM_OPERACAO")));
    }

    @Test
    void deveRejeitarStatusInvalidoNoCorpoDaRequisicao() throws Exception {
        mockMvc.perform(patch("/api/motoristas/{id}/status", motoristaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private VeiculoTipo criarTipo(String nome) {
        VeiculoTipo tipo = new VeiculoTipo();
        tipo.setNome(nome);
        return tipo;
    }

    private Veiculo criarVeiculo(String placa, String marca, VeiculoTipo tipo) {
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setMarca(marca);
        veiculo.setTipo(tipo);
        return veiculo;
    }
}
