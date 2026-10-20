package br.com.newelog.motoristas.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.controller.ManifestoValidacaoDTO;

@Service
public class ManifestoValidacaoService {

    

    public void validar(ManifestoValidacaoDTO manifesto) {
        List<String> erros = new ArrayList<>();

        if (!CpfCnpjValidator.isValid(manifesto.cpfMotorista())) {
            erros.add("cpfMotorista inválido: " + manifesto.cpfMotorista());
        }

        String documentoAgregado = manifesto.cpfCnpjAgregado();
        if (documentoAgregado != null
                && !documentoAgregado.isBlank()
                && !CpfCnpjValidator.isValid(documentoAgregado)) {
            erros.add("cpfCnpjAgregado inválido");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }
}