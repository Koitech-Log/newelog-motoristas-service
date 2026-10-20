package br.com.newelog.motoristas.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.dto.MotoristaDetalheDTO;
import br.com.newelog.motoristas.model.Motorista;
import br.com.newelog.motoristas.model.StatusMotorista;
import br.com.newelog.motoristas.model.Veiculo;
import br.com.newelog.motoristas.model.Viagem;
import br.com.newelog.motoristas.repository.MotoristaRepository;
import br.com.newelog.motoristas.repository.VeiculoRepository;

/**
 * Efetiva o cadastro/atualização de um motorista a partir de uma linha de
 * manifesto já validada por ManifestoValidacaoService. O casamento com um
 * motorista já cadastrado é feito por CPF/CNPJ — se já existir, nome, placa
 * e status são atualizados com o que veio no manifesto (regra combinada com
 * o parceiro: o manifesto é a fonte mais recente desses dados); se não
 * existir, o motorista é criado com cadastroValidado=false, para sinalizar
 * que ainda não passou por validação manual do cadastro.
 */
@Service
public class ManifestoCadastroService {

    private static final String ORIGEM_FIXA = "São José dos Campos";
    private static final String DESTINO_NAO_INFORMADO = "Não informado";

    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaService motoristaService;

    public ManifestoCadastroService(
            MotoristaRepository motoristaRepository,
            VeiculoRepository veiculoRepository,
            MotoristaService motoristaService
    ) {
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaService = motoristaService;
    }

    @Transactional
    public MotoristaDetalheDTO cadastrar(ManifestoCadastroDTO dados) {
        String cpfNormalizado = formatarCpfCnpj(dados.cpfMotorista());
        var existente = motoristaRepository.findByCpfCnpj(cpfNormalizado);
        boolean jaExistia = existente.isPresent();
        Motorista motorista = existente.orElseGet(Motorista::new);

        if (!jaExistia) {
            motorista.setCodigoExterno(dados.codigoExterno());
            motorista.setCpfCnpj(cpfNormalizado);
            motorista.setCadastroValidado(false);
            motorista.setDiasDisponiveis(0);
            motorista.setDiasOperacao(0);
        }

        // Nome, placa e status são sempre atualizados com o dado mais recente
        // do manifesto — mesmo para motorista já existente (ver javadoc da classe).
        motorista.setNome(dados.nomeMotorista());
        motorista.setStatus(mapearStatus(dados.status()));
        motorista.setVeiculo(resolverVeiculo(dados.placaVeiculo()));

        BigDecimal valorFrete = dados.valorFrete() != null ? BigDecimal.valueOf(dados.valorFrete()) : BigDecimal.ZERO;

        // Evita duplicar a viagem se o mesmo manifesto (ou uma linha dele) for
        // importado mais de uma vez: mesma data + mesmo valor de frete para o
        // mesmo motorista é tratado como a mesma viagem, já que a Viagem não
        // guarda um identificador de origem do manifesto (ver javadoc de Viagem).
        boolean viagemJaExiste = motorista.getViagens().stream().anyMatch(v ->
                dados.data().equals(v.getData()) && valorFrete.compareTo(v.getValorFrete()) == 0
        );

        if (!viagemJaExiste) {
            Viagem viagem = new Viagem();
            viagem.setMotorista(motorista);
            viagem.setData(dados.data());
            viagem.setOrigem(ORIGEM_FIXA);
            viagem.setDestino(resolverDestino(motorista));
            viagem.setValorFrete(valorFrete);
            viagem.setValorPedagio(BigDecimal.ZERO);
            motorista.getViagens().add(viagem);
        }

        Motorista salvo = motoristaRepository.save(motorista);
        return motoristaService.buscarDetalhe(salvo.getId());
    }

    private Veiculo resolverVeiculo(String placa) {
        if (!StringUtils.hasText(placa)) return null;
        String placaNormalizada = placa.trim().toUpperCase();
        return veiculoRepository.findByPlaca(placaNormalizada)
                .orElseGet(() -> {
                    Veiculo novo = new Veiculo();
                    novo.setPlaca(placaNormalizada);
                    return veiculoRepository.save(novo);
                });
    }

    /**
     * O manifesto real traz status em formatos variados. Qualquer coisa que
     * não indique claramente operação em andamento é tratada como disponível.
     */
    private StatusMotorista mapearStatus(String statusBruto) {
        if (!StringUtils.hasText(statusBruto)) return StatusMotorista.DISPONIVEL;
        String normalizado = statusBruto.trim().toUpperCase();
        boolean emOperacao = normalizado.contains("OPERACAO") || normalizado.contains("OPERAÇÃO")
                || normalizado.contains("VIAGEM") || normalizado.contains("ANDAMENTO");
        return emOperacao ? StatusMotorista.EM_OPERACAO : StatusMotorista.DISPONIVEL;
    }

    /**
     * O CSV do manifesto não traz destino da viagem. O destino usado é o
     * mesmo da última viagem já registrada desse motorista — mesmo valor
     * "Não informado" já usado no seed real (V2) para os casos em que o
     * manifesto de origem não trouxe destino confiável.
     */
    private String resolverDestino(Motorista motorista) {
        var viagens = motorista.getViagens();
        if (!viagens.isEmpty()) {
            return viagens.get(viagens.size() - 1).getDestino();
        }
        return DESTINO_NAO_INFORMADO;
    }

    /**
     * O manifesto-service envia o CPF só com dígitos (ExtratorManifestoCsvService
     * usa limparDocumento). Aqui é reformatado para bater com o padrão já
     * salvo no banco (ver MotoristaSpecifications, que compara CPF formatado).
     */
    private String formatarCpfCnpj(String documento) {
        String digitos = documento.replaceAll("[^0-9]", "");
        if (digitos.length() == 11) {
            return digitos.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (digitos.length() == 14) {
            return digitos.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return documento;
    }
}
