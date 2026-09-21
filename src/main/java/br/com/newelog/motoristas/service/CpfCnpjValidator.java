package br.com.newelog.motoristas.service;

public class CpfCnpjValidator {

    public static boolean isValid(String valor) {
        if (valor == null) return false;
        String digits = valor.replaceAll("\\D", "");
        if (digits.length() == 11) return isCpfValido(digits);
        if (digits.length() == 14) return isCnpjValido(digits);
        return false;
    }

    private static boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;
        int[] n = cpf.chars().map(c -> c - '0').toArray();

        int soma = 0;
        for (int i = 0; i < 9; i++) soma += n[i] * (10 - i);
        int dv1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        if (dv1 != n[9]) return false;

        soma = 0;
        for (int i = 0; i < 10; i++) soma += n[i] * (11 - i);
        int dv2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        return dv2 == n[10];
    }

    private static boolean isCnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;
        int[] n = cnpj.chars().map(c -> c - '0').toArray();
        int[] p1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] p2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};

        int soma = 0;
        for (int i = 0; i < 12; i++) soma += n[i] * p1[i];
        int dv1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        if (dv1 != n[12]) return false;

        soma = 0;
        for (int i = 0; i < 13; i++) soma += n[i] * p2[i];
        int dv2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        return dv2 == n[13];
    }
}