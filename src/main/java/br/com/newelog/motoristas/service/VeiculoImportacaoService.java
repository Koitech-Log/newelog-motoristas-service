package br.com.newelog.motoristas.service;

import java.util.ArrayList;
import java.util.List;

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
 * Versão mínima da US09: uma linha só é gravada se tiver placa, se a placa ainda
 * não existir (no banco ou em uma linha anterior do mesmo arquivo) e se o tipo
 * informado existir. Essas checagens evitam erros de banco (placa única, chave
 * estrangeira e tamanho das colunas). As regras completas de validação — campos
 * obrigatórios, formato da placa e demais — são da US10.
 *
 * A placa é normalizada como em ManifestoCadastroService (trim + maiúsculas).
 * Uma linha inválida só entra na lista de erros e não impede as demais. Um erro
 * inesperado de banco desfaz o lote inteiro (a operação é uma única transação).
 */
@Service
public class VeiculoImportacaoService {

    private static final int TAMANHO_MAXIMO_PLACA = 10;
    private static final int TAMANHO_MAXIMO_MARCA = 60;

    private final VeiculoRepository veiculoRepository;
    private final VeiculoTipoRepository veiculoTipoRepository;

    public VeiculoImportacaoService(
            VeiculoRepository veiculoRepository,
            VeiculoTipoRepository veiculoTipoRepository
    ) {
        this.veiculoRepository = veiculoRepository;
        this.veiculoTipoRepository = veiculoTipoRepository;
    }

    @Transactional
    public ResultadoImportacaoVeiculosDTO importar(List<ImportacaoVeiculosRequestDTO.Item> itens) {
        int cadastrados = 0;
        List<ResultadoImportacaoVeiculosDTO.Erro> erros = new ArrayList<>();

        for (ImportacaoVeiculosRequestDTO.Item item : itens) {
            String placa = limpar(item.placa());
            String marca = limpar(item.marca());
            String tipoNome = limpar(item.tipo());

            if (placa == null) {
                erros.add(erro(item, "Placa não informada."));
                continue;
            }
            placa = placa.toUpperCase();

            if (placa.length() > TAMANHO_MAXIMO_PLACA) {
                erros.add(erro(item, "Placa com mais de " + TAMANHO_MAXIMO_PLACA + " caracteres: " + placa));
                continue;
            }
            if (marca != null && marca.length() > TAMANHO_MAXIMO_MARCA) {
                erros.add(erro(item, "Marca com mais de " + TAMANHO_MAXIMO_MARCA + " caracteres."));
                continue;
            }
            if (veiculoRepository.existsByPlaca(placa)) {
                erros.add(erro(item, "Placa já cadastrada: " + placa));
                continue;
            }

            VeiculoTipo tipo = null;
            if (tipoNome != null) {
                tipo = veiculoTipoRepository.findByNomeIgnoreCase(tipoNome).orElse(null);
                if (tipo == null) {
                    erros.add(erro(item, "Tipo de veículo não cadastrado: " + tipoNome));
                    continue;
                }
            }

            Veiculo veiculo = new Veiculo();
            veiculo.setPlaca(placa);
            veiculo.setMarca(marca);
            veiculo.setTipo(tipo);
            veiculoRepository.save(veiculo);
            cadastrados++;
        }

        return new ResultadoImportacaoVeiculosDTO(cadastrados, erros);
    }

    private ResultadoImportacaoVeiculosDTO.Erro erro(ImportacaoVeiculosRequestDTO.Item item, String motivo) {
        return new ResultadoImportacaoVeiculosDTO.Erro(item.linha(), motivo);
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}