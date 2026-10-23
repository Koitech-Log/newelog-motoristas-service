package br.com.newelog.motoristas.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import jakarta.validation.Validator;

@Service
public class VeiculoValidacaoService {

    public record Violacao(String campo, String motivo) {
    }

    private static final Map<String, String> ROTULOS = Map.of(
            "placa", "Placa",
            "marca", "Marca",
            "tipo", "Tipo Veículo");

    private final Validator validator;

    public VeiculoValidacaoService(Validator validator) {
        this.validator = validator;
    }

    public List<Violacao> validarComCampo(ImportacaoVeiculosRequestDTO.Item item) {
        return validator.validate(item).stream()
                .map(v -> {
                    String caminho = v.getPropertyPath().toString();
                    return new Violacao(ROTULOS.getOrDefault(caminho, caminho), v.getMessage());
                })
                .sorted(Comparator.comparing(Violacao::motivo))
                .toList();
    }

    public List<String> validar(ImportacaoVeiculosRequestDTO.Item item) {
        return validarComCampo(item).stream().map(Violacao::motivo).toList();
    }
}