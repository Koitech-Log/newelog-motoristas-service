package br.com.newelog.motoristas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import br.com.newelog.motoristas.dto.ResultadoImportacaoVeiculosDTO;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.model.VeiculoTipo;
import br.com.newelog.motoristas.repository.VeiculoRepository;
import br.com.newelog.motoristas.repository.VeiculoTipoRepository;

/**
 * Testa a validação e a gravação em lote de veículos direto no service, com o
 * banco H2 do perfil de teste. Os tipos de veículo são criados em cada teste
 * (a V2, com o seed, não roda no perfil de teste) e o @Transactional desfaz
 * tudo ao final.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VeiculoImportacaoServiceTest {

    @Autowired
    private VeiculoImportacaoService service;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private VeiculoTipoRepository veiculoTipoRepository;

    private long veiculosAntes;

    @BeforeEach
    void setUp() {
        veiculoTipoRepository.save(criarTipo("VUC"));
        veiculoTipoRepository.save(criarTipo("Truck"));
        veiculosAntes = veiculoRepository.count();
    }

    @Test
    void deveCadastrarLinhasValidas() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "VUC"),
                item(3, "DEF1234", "Volvo", "Truck")));

        assertEquals(2, resultado.cadastrados());
        assertEquals(0, resultado.atualizados());
        assertTrue(resultado.erros().isEmpty());
        assertEquals(veiculosAntes + 2, veiculoRepository.count());

        Veiculo primeiro = veiculoRepository.findByPlaca("ABC1D23").orElseThrow();
        assertEquals("Fiat", primeiro.getMarca());
        assertEquals("VUC", primeiro.getTipo().getNome());
    }

    @Test
    void deveRejeitarLinhaSemPlaca() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, null, "Fiat", "VUC")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertEquals(2, resultado.erros().get(0).linha());
        assertTrue(resultado.erros().get(0).motivo().contains("placa é obrigatória"));
        assertEquals("Placa", resultado.erros().get(0).campo());
        assertEquals(veiculosAntes, veiculoRepository.count());
    }

    @Test
    void deveRejeitarLinhaSemTipo() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", null)));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertTrue(resultado.erros().get(0).motivo().contains("tipo é obrigatório"));
        assertEquals(veiculosAntes, veiculoRepository.count());
    }

    @Test
    void deveRejeitarPlacaEmFormatoInvalido() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC12", "Fiat", "VUC")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertTrue(resultado.erros().get(0).motivo().contains("formato inválido"));
        assertEquals(veiculosAntes, veiculoRepository.count());
    }

    @Test
    void deveRejeitarPlacaRepetidaNoArquivoMantendoAPrimeira() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "VUC"),
                item(3, "abc1d23", "Volvo", "Truck")));

        assertEquals(1, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertEquals(3, resultado.erros().get(0).linha());
        assertTrue(resultado.erros().get(0).motivo().contains("repetida"));
        assertTrue(resultado.erros().get(0).motivo().contains("linha 2"));
        assertEquals(veiculosAntes + 1, veiculoRepository.count());

        assertEquals("Fiat", veiculoRepository.findByPlaca("ABC1D23").orElseThrow().getMarca());
    }

    @Test
    void deveRejeitarTipoNaoCadastrado() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "Foguete")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertTrue(resultado.erros().get(0).motivo().contains("Foguete"));
        assertEquals(veiculosAntes, veiculoRepository.count());
    }

    @Test
    void deveAtualizarMarcaETipoQuandoAPlacaJaExiste() {
        veiculoRepository.save(criarVeiculo("ABC1D23", null));

        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "VUC")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.atualizados());
        assertTrue(resultado.erros().isEmpty());
        assertEquals(veiculosAntes + 1, veiculoRepository.count()); // não criou duplicata

        Veiculo atualizado = veiculoRepository.findByPlaca("ABC1D23").orElseThrow();
        assertEquals("Fiat", atualizado.getMarca());
        assertEquals("VUC", atualizado.getTipo().getNome());
    }

    @Test
    void naoDeveApagarAMarcaQuandoOCsvVemSemMarca() {
        veiculoRepository.save(criarVeiculo("ABC1D23", "Iveco"));

        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", null, "VUC")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.atualizados());

        Veiculo atualizado = veiculoRepository.findByPlaca("ABC1D23").orElseThrow();
        assertEquals("Iveco", atualizado.getMarca());
        assertEquals("VUC", atualizado.getTipo().getNome());
    }

    @Test
    void naoDeveAlterarVeiculoExistenteQuandoALinhaEInvalida() {
        veiculoRepository.save(criarVeiculo("ABC1D23", "Iveco"));

        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "Foguete")));

        assertEquals(0, resultado.cadastrados());
        assertEquals(1, resultado.erros().size());
        assertEquals("Iveco", veiculoRepository.findByPlaca("ABC1D23").orElseThrow().getMarca());
    }

    @Test
    void deveProcessarOsValidosMesmoComLinhasInvalidasNoArquivo() {
        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "VUC"),        // válida
                item(3, null, "Volvo", "Truck"),          // sem placa
                item(4, "XYZ9K88", "Scania", "Foguete"),  // tipo inexistente
                item(5, "DEF2G34", "Mercedes", "Truck"))); // válida

        assertEquals(2, resultado.cadastrados());
        assertEquals(2, resultado.erros().size());
        assertEquals(3, resultado.erros().get(0).linha());
        assertEquals(4, resultado.erros().get(1).linha());
        assertEquals(veiculosAntes + 2, veiculoRepository.count());
    }

    @Test
    void deveSepararOsVeiculosNovosDosQueJaExistiam() {
        veiculoRepository.save(criarVeiculo("ABC1D23", "Iveco"));

        ResultadoImportacaoVeiculosDTO resultado = service.importar(List.of(
                item(2, "ABC1D23", "Fiat", "VUC"),        // já existia
                item(3, "DEF1234", "Volvo", "Truck"),     // novo
                item(4, "XYZ9K88", "Scania", "Foguete"))); // erro: tipo inexistente

        assertEquals(1, resultado.cadastrados());
        assertEquals(1, resultado.atualizados());
        assertEquals(1, resultado.erros().size());
        assertEquals(4, resultado.erros().get(0).linha());
    }

    private ImportacaoVeiculosRequestDTO.Item item(int linha, String placa, String marca, String tipo) {
        return new ImportacaoVeiculosRequestDTO.Item(linha, placa, marca, tipo);
    }

    private VeiculoTipo criarTipo(String nome) {
        VeiculoTipo tipo = new VeiculoTipo();
        tipo.setNome(nome);
        return tipo;
    }

    private Veiculo criarVeiculo(String placa, String marca) {
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placa);
        veiculo.setMarca(marca);
        return veiculo;
    }
}