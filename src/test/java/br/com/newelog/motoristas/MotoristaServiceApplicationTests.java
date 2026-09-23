package br.com.newelog.motoristas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Garante que o contexto Spring sobe corretamente, incluindo a migration V1
 * do Flyway contra o banco H2 do perfil de teste.
 */
@SpringBootTest
@ActiveProfiles("test")
class MotoristasServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
