package br.com.newelog.motoristas.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.dto.ImportacaoVeiculosRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class VeiculoValidacaoService {

    private final Validator validator;

    public VeiculoValidacaoService(Validator validator) {
        this.validator = validator;
    }

    public List<String> validar(ImportacaoVeiculosRequestDTO.Item item) {
        return validator.validate(item).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();
    }
}