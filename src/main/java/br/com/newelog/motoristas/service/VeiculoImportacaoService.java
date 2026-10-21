package br.com.newelog.motoristas.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import br.com.newelog.motoristas.dto.ResultadoImportacaoVeiculosDTO;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.model.VeiculoTipo;
import br.com.newelog.motoristas.repository.VeiculoRepository;
import br.com.newelog.motoristas.repository.VeiculoTipoRepository;

/**
 * Cadastra veículos a partir das linhas de um CSV já lido pelo manifesto-service.
 *
 * A importação tem duas fases:
 *   1. Validação: todas as linhas são validadas antes da gravação. Uma linha
 *      inválida só entra na lista de erros, com o motivo, e não impede as demais.
 *   2. Gravação: todos os veículos válidos são salvos de uma só vez, com
 *      saveAll, dentro da mesma transação.
 *
 * A validação de cada linha tem duas partes:
 *   - campos obrigatórios e formato (placa, tamanho da marca, tipo informado):
 *     regras do próprio ImportacaoVeiculosRequestDTO.Item, aplicadas por
 *     VeiculoValidacaoService;
 *   - regras que dependem do arquivo e do banco: placa repetida no mesmo
 *     arquivo e tipo de veículo cadastrado.
 *
 * Se a placa já existe no banco, o veículo é atualizado com a marca e o tipo
 * do CSV, em vez de ser rejeitado. Isso completa os veículos criados por
 * importação de manifesto, que só têm a placa. Marca em branco no CSV não apaga
 * a marca que já está salva. O total "cadastrados" da resposta conta tanto os
 * veículos novos quanto os atualizados.
 *
 * A placa é normalizada como em ManifestoCadastroService (trim + maiúsculas).
 * Um erro inesperado de banco na gravação desfaz o lote inteiro.
 */
@Service
public class VeiculoImportacaoService {

    private final VeiculoRepository veiculoRepository;
    private final VeiculoTipoRepository veiculoTipoRepository;
    private final VeiculoValidacaoService validacaoService;

    public VeiculoImportacaoService(
            VeiculoRepository veiculoRepository,
            VeiculoTipoRepository veiculoTipoRepository,
            VeiculoValidacaoService validacaoService
    ) {
        this.veiculoRepository = veiculoRepository;
        this.veiculoTipoRepository = veiculoTipoRepository;
        this.validacaoService = validacaoService;
    }

    @Transactional
    public ResultadoImportacaoVeiculosDTO importar(List<ImportacaoVeiculosRequestDTO.Item> itens) {
        List<ResultadoImportacaoVeiculosDTO.Erro> erros = new ArrayList<>();
        List<Veiculo> validos = new ArrayList<>();

        // placa -> linha em que foi aceita, para apontar repetidas no mesmo arquivo
        Map<String, Integer> linhaPorPlaca = new HashMap<>();
        // evita consultar o mesmo tipo várias vezes (inclusive os que não existem)
        Map<String, Optional<VeiculoTipo>> tiposPorNome = new HashMap<>();

        // Fase 1: valida todas as linhas.
        for (ImportacaoVeiculosRequestDTO.Item item : itens) {
            Veiculo veiculo = validar(item, linhaPorPlaca, tiposPorNome, erros);
            if (veiculo != null) {
                validos.add(veiculo);
                linhaPorPlaca.put(veiculo.getPlaca(), item.linha());
            }
        }

        // Fase 2: grava de uma só vez todos os veículos válidos (novos e atualizados).
        List<Veiculo> salvos = veiculoRepository.saveAll(validos);

        return new ResultadoImportacaoVeiculosDTO(salvos.size(), erros);
    }

    /**
     * Valida uma linha. Se estiver correta, devolve o veículo pronto para ser
     * salvo (novo, ou o já cadastrado com a marca e o tipo atualizados); se não,
     * registra o motivo em "erros" e devolve null. O veículo existente só é
     * alterado depois que todas as validações da linha passam.
     */
    private Veiculo validar(
            ImportacaoVeiculosRequestDTO.Item item,
            Map<String, Integer> linhaPorPlaca,
            Map<String, Optional<VeiculoTipo>> tiposPorNome,
            List<ResultadoImportacaoVeiculosDTO.Erro> erros
    ) {
        // Campos obrigatórios e formato (regras do Item). Os motivos vêm
        // separados por "; " para ficarem legíveis quando há mais de um.
        List<String> motivos = validacaoService.validar(item);
        if (!motivos.isEmpty()) {
            erros.add(erro(item, String.join("; ", motivos)));
            return null;
        }

        // Daqui em diante a placa está preenchida e no formato esperado.
        String placa = item.placa().trim().toUpperCase();
        String marca = limpar(item.marca());
        String tipoNome = limpar(item.tipo());

        Integer linhaAnterior = linhaPorPlaca.get(placa);
        if (linhaAnterior != null) {
            erros.add(erro(item, "Placa repetida no arquivo: " + placa
                    + " (já informada na linha " + linhaAnterior + ")."));
            return null;
        }

        VeiculoTipo tipo = null;
        if (tipoNome != null) {
            tipo = tiposPorNome
                    .computeIfAbsent(tipoNome.toLowerCase(Locale.ROOT),
                            chave -> veiculoTipoRepository.findByNomeIgnoreCase(tipoNome))
                    .orElse(null);
            if (tipo == null) {
                erros.add(erro(item, "Tipo de veículo não cadastrado: " + tipoNome));
                return null;
            }
        }

        // Todas as validações passaram: só agora o veículo existente é alterado.
        Veiculo veiculo = veiculoRepository.findByPlaca(placa).orElseGet(Veiculo::new);
        veiculo.setPlaca(placa);
        if (marca != null) veiculo.setMarca(marca);
        if (tipo != null) veiculo.setTipo(tipo);
        return veiculo;
    }

    private ResultadoImportacaoVeiculosDTO.Erro erro(ImportacaoVeiculosRequestDTO.Item item, String motivo) {
        return new ResultadoImportacaoVeiculosDTO.Erro(item.linha(), motivo);
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}