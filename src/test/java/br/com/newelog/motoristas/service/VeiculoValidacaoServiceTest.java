package br.com.newelog.motoristas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import jakarta.validation.Validation;

class VeiculoValidacaoServiceTest {

    private final VeiculoValidacaoService servico =
            new VeiculoValidacaoService(Validation.buildDefaultValidatorFactory().getValidator());

    private ImportacaoVeiculosRequestDTO.Item item(String placa, String marca, String tipo) {
        return new ImportacaoVeiculosRequestDTO.Item(2, placa, marca, tipo);
    }

    @Test
    void aceitaPlacaNoFormatoAntigoENoMercosul() {
        assertTrue(servico.validar(item("ABC1234", "Fiat", "VUC")).isEmpty());
        assertTrue(servico.validar(item("ABC1D23", "Fiat", "VUC")).isEmpty());
        assertTrue(servico.validar(item("abc1d23", "Fiat", "VUC")).isEmpty());
        assertTrue(servico.validar(item("ABC-1234", "Fiat", "VUC")).isEmpty());
    }

    @Test
    void marcaEOpcionalSoPlacaETipoSaoObrigatorios() {
        assertTrue(servico.validar(item("ABC1D23", null, "VUC")).isEmpty());
    }

    @Test
    void placaNulaGeraSoOMotivoDeObrigatoria() {
        assertEquals(List.of("placa é obrigatória"), servico.validar(item(null, "Fiat", "VUC")));
    }

    @Test
    void placaEmBrancoNaoEAceita() {
        assertTrue(servico.validar(item("   ", "Fiat", "VUC")).contains("placa é obrigatória"));
    }

    @Test
    void tipoFaltandoGeraMotivoDeTipoObrigatorio() {
        assertEquals(List.of("tipo é obrigatório"), servico.validar(item("ABC1D23", "Fiat", null)));
        assertEquals(List.of("tipo é obrigatório"), servico.validar(item("ABC1D23", "Fiat", "  ")));
    }

    @Test
    void rejeitaPlacaEmFormatoInvalido() {
        for (String placa : List.of("1234ABC", "ABCD123", "AB-C1234", "ABC12", "ABC 1234")) {
            List<String> motivos = servico.validar(item(placa, "Fiat", "VUC"));

            assertEquals(1, motivos.size(), "placa: " + placa);
            assertTrue(motivos.get(0).contains("formato inválido"), "placa: " + placa);
        }
    }

    @Test
    void marcaComAte60CaracteresEAceitaEComMaisDe60EInvalida() {
        assertTrue(servico.validar(item("ABC1D23", "M".repeat(60), "VUC")).isEmpty());

        assertEquals(List.of("marca deve ter no máximo 60 caracteres"),
                servico.validar(item("ABC1D23", "M".repeat(61), "VUC")));
    }

    @Test
    void variosProblemasNaMesmaLinhaMostramTodosOsMotivos() {
        List<String> motivos = servico.validar(item(null, "M".repeat(61), null));

        assertEquals(3, motivos.size());
        assertTrue(motivos.contains("placa é obrigatória"));
        assertTrue(motivos.contains("marca deve ter no máximo 60 caracteres"));
        assertTrue(motivos.contains("tipo é obrigatório"));
    }
}