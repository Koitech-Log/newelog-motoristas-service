package br.com.newelog.motoristas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VeiculoSchemaTest {

    private static final String INSERT_VEICULO =
            "INSERT INTO veiculo (placa, marca, veiculo_tipo_id) VALUES (?, ?, ?)";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bancoAceitaVeiculoSoComPlaca() {
        // É o caso dos veículos criados pela importação de manifesto.
        jdbc.update(INSERT_VEICULO, "ABC1D23", null, null);

        assertEquals(1L, contarVeiculos("ABC1D23"));
    }

    @Test
    void bancoAceitaVeiculoComMarcaETipoExistente() {
        jdbc.update("INSERT INTO veiculo_tipo (nome) VALUES (?)", "VUC");
        Long tipoId = jdbc.queryForObject("SELECT id FROM veiculo_tipo WHERE nome = ?", Long.class, "VUC");

        jdbc.update(INSERT_VEICULO, "ABC1D23", "Fiat", tipoId);

        assertEquals(1L, contarVeiculos("ABC1D23"));
    }

    @Test
    void bancoRejeitaVeiculoSemPlaca() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT_VEICULO, null, "Fiat", null));
    }

    @Test
    void bancoRejeitaPlacaRepetida() {
        jdbc.update(INSERT_VEICULO, "ABC1D23", "Fiat", null);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT_VEICULO, "ABC1D23", "Volvo", null));
    }

    @Test
    void bancoRejeitaPlacaComMaisDe10Caracteres() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT_VEICULO, "ABCDEFGHIJK", null, null));
    }

    @Test
    void bancoRejeitaMarcaComMaisDe60Caracteres() {
        String marcaLonga = "M".repeat(61);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT_VEICULO, "ABC1D23", marcaLonga, null));
    }

    @Test
    void bancoRejeitaVeiculoComTipoInexistente() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(INSERT_VEICULO, "ABC1D23", "Fiat", 999999L));
    }

    private long contarVeiculos(String placa) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM veiculo WHERE placa = ?", Long.class, placa);
        return total == null ? 0L : total;
    }
}