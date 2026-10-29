package br.com.newelog.motoristas.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.model.VeiculoTipo;
import br.com.newelog.motoristas.repository.VeiculoRepository;
import br.com.newelog.motoristas.repository.VeiculoTipoRepository;

/**
 * Testa POST /api/veiculos/cadastrar-de-csv com JWT real.
 *
 * GESTOR e OPERADOR podem importar veículos por CSV.
 * O teste valida autenticação, importação e persistência.
 *
 * Os tipos de veículo são criados nos testes.
 * O @Transactional desfaz as alterações ao final de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VeiculoControllerTest {

    private static final String SEGREDO =
            "segredo-de-teste-com-mais-de-32-caracteres-ok";

    private static final String URL =
            "/api/veiculos/cadastrar-de-csv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private VeiculoTipoRepository veiculoTipoRepository;

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoComUmVeiculo("ABC1D23")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorPodeImportarVeiculoPorCsv() throws Exception {
        veiculoTipoRepository.save(criarTipo("VUC"));

        String corpo =
                "{\"veiculos\":[{\"linha\":2,\"placa\":\"abc1d23\","
                + "\"marca\":\"Fiat\",\"tipo\":\"vuc\"}]}";

        mockMvc.perform(post(URL)
                        .header(
                                "Authorization",
                                "Bearer " + token("OPERADOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastrados", is(1)))
                .andExpect(jsonPath("$.atualizados", is(0)))
                .andExpect(jsonPath("$.erros.length()", is(0)));

        Veiculo salvo = veiculoRepository
                .findByPlaca("ABC1D23")
                .orElseThrow();

        assertEquals("Fiat", salvo.getMarca());
        assertEquals("VUC", salvo.getTipo().getNome());
    }

    @Test
    void gestorPodeImportarVeiculoPorCsv() throws Exception {
        veiculoTipoRepository.save(criarTipo("VUC"));

        String corpo =
                "{\"veiculos\":[{\"linha\":2,\"placa\":\"def4g56\","
                + "\"marca\":\"Volvo\",\"tipo\":\"VUC\"}]}";

        mockMvc.perform(post(URL)
                        .header(
                                "Authorization",
                                "Bearer " + token("GESTOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastrados", is(1)))
                .andExpect(jsonPath("$.atualizados", is(0)))
                .andExpect(jsonPath("$.erros.length()", is(0)));

        Veiculo salvo = veiculoRepository
                .findByPlaca("DEF4G56")
                .orElseThrow();

        assertEquals("Volvo", salvo.getMarca());
        assertEquals("VUC", salvo.getTipo().getNome());
    }

    @Test
    void listaDeVeiculosVaziaRetorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .header(
                                "Authorization",
                                "Bearer " + token("GESTOR")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"veiculos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    private String corpoComUmVeiculo(String placa) {
        return "{\"veiculos\":[{\"linha\":2,\"placa\":\""
                + placa + "\"}]}";
    }

    private VeiculoTipo criarTipo(String nome) {
        VeiculoTipo tipo = new VeiculoTipo();
        tipo.setNome(nome);
        return tipo;
    }

    private static String token(String perfil) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .issuer("newelog-auth-service")
                        .subject("1")
                        .claim("perfil", perfil)
                        .expirationTime(
                                Date.from(Instant.now().plusSeconds(300))
                        )
                        .build()
        );

        jwt.sign(
                new MACSigner(
                        SEGREDO.getBytes(StandardCharsets.UTF_8)
                )
        );

        return jwt.serialize();
    }
}
