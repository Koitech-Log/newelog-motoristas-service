package br.com.newelog.motoristas.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.controller.ManifestoValidacaoDTO;

@Service
public class ManifestoValidacaoService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // aceita placa antiga (ABC1234) e Mercosul (ABC1D23)
    private static final Pattern FORMATO_PLACA = Pattern.compile("^[A-Z]{3}[0-9]([0-9]{3}|[A-Z][0-9]{2})$");
    private static final double TOLERANCIA = 0.01;

    public void validar(ManifestoValidacaoDTO manifesto) {
        List<String> erros = new ArrayList<>();

        obrigatorio(manifesto.manifestoID(), "manifestoID", erros);
        obrigatorio(manifesto.nomeMotorista(), "nomeMotorista", erros);
        obrigatorio(manifesto.nomeAgregado(), "nomeAgregado", erros);

        validarData(manifesto.data(), erros);
        validarPlaca(manifesto.placaVeiculo(), erros);
        validarCpfCnpj(manifesto.cpfMotorista(), "cpfMotorista", erros);
        validarCpfCnpj(manifesto.cpfCnpjAgregado(), "cpfCnpjAgregado", erros);
        validarValores(manifesto, erros);

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    private void obrigatorio(String valor, String campo, List<String> erros) {
        if (valor == null || valor.isBlank()) erros.add(campo + " é obrigatório");
    }

    private void validarCpfCnpj(String valor, String campo, List<String> erros) {
        if (valor == null || valor.isBlank()) {
            erros.add(campo + " é obrigatório");
        } else if (!CpfCnpjValidator.isValid(valor)) {
            erros.add(campo + " inválido: " + valor);
        }
    }

    private void validarData(String data, List<String> erros) {
        if (data == null || data.isBlank()) {
            erros.add("data é obrigatória");
            return;
        }
        try {
            LocalDate.parse(data, FORMATO_DATA);
        } catch (DateTimeParseException e) {
            erros.add("data em formato inválido (esperado yyyy-MM-dd): " + data);
        }
    }

    private void validarPlaca(String placa, List<String> erros) {
        if (placa == null || placa.isBlank()) {
            erros.add("placaVeiculo é obrigatório");
            return;
        }
        String placaLimpa = placa.replaceAll("[\\s-]", "").toUpperCase();
        if (!FORMATO_PLACA.matcher(placaLimpa).matches()) {
            erros.add("placaVeiculo em formato inválido: " + placa);
        }
    }

    private void validarValores(ManifestoValidacaoDTO manifesto, List<String> erros) {
        Double frete = manifesto.valorFrete();
        Double despesas = manifesto.totalDespesas();
        Double saldo = manifesto.saldoAPagar();

        if (frete == null) erros.add("valorFrete é obrigatório");
        else if (frete < 0) erros.add("valorFrete não pode ser negativo: " + frete);

        if (despesas == null) erros.add("totalDespesas é obrigatório");
        else if (despesas < 0) erros.add("totalDespesas não pode ser negativo: " + despesas);

        if (saldo == null) erros.add("saldoAPagar é obrigatório");
        else if (saldo < 0) erros.add("saldoAPagar não pode ser negativo: " + saldo);

        if (frete != null && despesas != null && saldo != null) {
            double esperado = frete - despesas;
            if (Math.abs(saldo - esperado) > TOLERANCIA) {
                erros.add(String.format(
                    "saldoAPagar (%.2f) não bate com valorFrete - totalDespesas (%.2f)",
                    saldo, esperado));
            }
        }
    }
}