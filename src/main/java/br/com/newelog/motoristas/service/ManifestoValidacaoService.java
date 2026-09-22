package br.com.newelog.motoristas.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.controller.ManifestoValidacaoDTO;

@Service
public class ManifestoValidacaoService {

    private static final double TOLERANCIA = 0.01;

    public void validar(ManifestoValidacaoDTO manifesto) {
        List<String> erros = new ArrayList<>();

        if (!CpfCnpjValidator.isValid(manifesto.cpfMotorista())) {
            erros.add("cpfMotorista inválido: " + manifesto.cpfMotorista());
        }
        if (!CpfCnpjValidator.isValid(manifesto.cpfCnpjAgregado())) {
            erros.add("cpfCnpjAgregado inválido: " + manifesto.cpfCnpjAgregado());
        }

        double esperado = manifesto.valorFrete() - manifesto.totalDespesas();
        if (Math.abs(manifesto.saldoAPagar() - esperado) > TOLERANCIA) {
            erros.add(String.format(
                "saldoAPagar (%.2f) não bate com valorFrete - totalDespesas (%.2f)",
                manifesto.saldoAPagar(), esperado));
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }
}