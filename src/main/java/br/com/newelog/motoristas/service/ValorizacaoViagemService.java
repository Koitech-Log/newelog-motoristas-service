package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Service;

import br.com.newelog.motoristas.config.ValorizacaoProperties;

@Service
public class ValorizacaoViagemService {

    private static final String SAO_PAULO = "sao paulo";

    private final ValorizacaoProperties props;

    public ValorizacaoViagemService(ValorizacaoProperties props) {
        this.props = props;
    }

    public BigDecimal aplicar(BigDecimal valorBase, String destino) {
        if (valorBase == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal valor = ehSaoPaulo(destino)
                ? valorBase.multiply(props.fatorSaoPaulo())
                : valorBase;
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean ehSaoPaulo(String destino) {
        if (destino == null) return false;
        String n = Normalizer.normalize(destino, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return n.equals(SAO_PAULO) || n.startsWith(SAO_PAULO + "/") || n.startsWith(SAO_PAULO + " -");
    }
}