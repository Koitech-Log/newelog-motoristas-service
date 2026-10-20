package br.com.newelog.motoristas.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Sem @WithMockUser: aqui o JWT é validado de verdade, ponta a ponta. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaJwtTest {

    // Mesmo valor de app.jwt.secret em application-test.properties
    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-ok";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void semTokenRetorna401ComCorpoPadrao() throws Exception {
        mockMvc.perform(get("/api/motoristas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem", Matchers.notNullValue()));
    }

    @Test
    void tokenValidoPassa() throws Exception {
        mockMvc.perform(get("/api/motoristas")
                        .header("Authorization", "Bearer " + token(SEGREDO, "newelog-auth-service", 300)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAssinadoComOutroSegredoRetorna401() throws Exception {
        String token = token("outro-segredo-totalmente-diferente-32-chars", "newelog-auth-service", 300);
        mockMvc.perform(get("/api/motoristas").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        mockMvc.perform(get("/api/motoristas")
                        .header("Authorization", "Bearer " + token(SEGREDO, "newelog-auth-service", -3600)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOutroEmissorRetorna401() throws Exception {
        mockMvc.perform(get("/api/motoristas")
                        .header("Authorization", "Bearer " + token(SEGREDO, "outro-emissor", 300)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthContinuaPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private static String token(String segredo, String emissor, long segundosAteExpirar) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder()
                .issuer(emissor)
                .subject("1")
                .claim("perfil", "GESTOR")
                .expirationTime(Date.from(Instant.now().plusSeconds(segundosAteExpirar)))
                .build());
        jwt.sign(new MACSigner(segredo.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
