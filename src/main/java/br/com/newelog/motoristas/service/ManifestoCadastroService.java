package br.com.newelog.motoristas.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
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
import br.com.newelog.motoristas.repository.ViagemRepository;

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

    private static final Logger log = LoggerFactory.getLogger(ManifestoCadastroService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private static final String ORIGEM_FIXA = "São José dos Campos";
    private static final String DESTINO_NAO_INFORMADO = "Não informado";

    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaService motoristaService;
    private final ViagemRepository viagemRepository;

    public ManifestoCadastroService(
            MotoristaRepository motoristaRepository,
            VeiculoRepository veiculoRepository,
            MotoristaService motoristaService,
            ViagemRepository viagemRepository
    ) {
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaService = motoristaService;
        this.viagemRepository = viagemRepository;
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

        BigDecimal valorFrete = dados.valorFrete() != null
                ? BigDecimal.valueOf(dados.valorFrete()) : BigDecimal.ZERO;
        BigDecimal valorPedagio = dados.valorPedagio();
        if (valorPedagio != null && (valorPedagio.signum() < 0
                || valorPedagio.scale() > 2
                || valorPedagio.compareTo(new BigDecimal("9999999999.99")) > 0)) {
            throw new ValidacaoException(java.util.List.of("valorPedagio fora de NUMERIC(12,2)"));
        }

        Correspondencia correspondencia =
                localizarViagem(motorista, dados.numeroManifesto(), dados.data(), valorFrete);
        Viagem viagemExistente = correspondencia.viagem();

        if (correspondencia.ambigua()) {
            // Sem número: não escolhe entre várias viagens; pedágio e destino ficam como estão
            if (valorPedagio != null || dados.destino() != null) {
                log.warn("Pedágio e destino preservados: várias viagens correspondem à linha importada");
            }
        } else if (viagemExistente == null) {
            Viagem viagem = new Viagem();
            viagem.setMotorista(motorista);
            viagem.setData(dados.data());
            viagem.setOrigem(ORIGEM_FIXA);
            viagem.setDestino(dados.destino() != null ? dados.destino() : DESTINO_NAO_INFORMADO);
            viagem.setValorFrete(valorFrete);
            viagem.setValorPedagio(valorPedagio != null ? valorPedagio : BigDecimal.ZERO);
            viagem.setNumeroManifesto(dados.numeroManifesto());
            motorista.getViagens().add(viagem);
        } else {
            corrigirDestino(viagemExistente, dados.destino());
            if (valorPedagio != null) {
                Viagem viagem = viagemExistente;
                // A condição no UPDATE impede sobrescrever um custo conhecido,
                // inclusive quando duas reimportações disputam o mesmo registro.
                int corrigidas = entityManager.createQuery(
                        "UPDATE Viagem v SET v.valorPedagio = :pedagio "
                        + "WHERE v.id = :id AND v.valorPedagio = 0")
                        .setParameter("pedagio", valorPedagio)
                        .setParameter("id", viagem.getId())
                        .executeUpdate();
                entityManager.refresh(viagem);
                if (corrigidas == 0 && valorPedagio.compareTo(viagem.getValorPedagio()) != 0) {
                    log.warn("Pedágio já preenchido foi preservado na viagem {}", viagem.getId());
                }
            }
        }

        Motorista salvo = motoristaRepository.save(motorista);
        return motoristaService.buscarDetalhe(salvo.getId());
    }

    /** viagem null e ambigua false = nenhuma viagem casa (criar nova). */
    private record Correspondencia(Viagem viagem, boolean ambigua) {
        static final Correspondencia NENHUMA = new Correspondencia(null, false);
        static final Correspondencia VARIAS = new Correspondencia(null, true);
        static Correspondencia de(Viagem viagem) { return new Correspondencia(viagem, false); }
    }

    /**
     * Identifica a viagem já gravada que corresponde à linha do manifesto.
     *
     * Com número do manifesto: 1º a viagem com esse número; se não houver,
     * uma viagem SEM número do mesmo motorista + data + frete (base gravada
     * antes da V3), que passa a ter o número; senão, nenhuma (cria nova).
     * Assim dois manifestos distintos com a mesma data e frete viram duas
     * viagens, e a base antiga não é duplicada na primeira reimportação.
     *
     * Sem número (cliente antigo): critério antigo motorista + data + frete;
     * 2 ou mais correspondentes = VARIAS (não altera nenhuma).
     */
    private Correspondencia localizarViagem(Motorista motorista, String numero, LocalDate data, BigDecimal valorFrete) {
        if (numero == null) {
            var correspondentes = correspondentesPeloCriterioAntigo(motorista, data, valorFrete).toList();
            if (correspondentes.isEmpty()) return Correspondencia.NENHUMA;
            return correspondentes.size() == 1 ? Correspondencia.de(correspondentes.get(0)) : Correspondencia.VARIAS;
        }

        var porNumero = viagemRepository.findByNumeroManifesto(numero);
        if (porNumero.isPresent()) {
            Viagem viagem = porNumero.get();
            if (motorista.getId() == null || !motorista.getId().equals(viagem.getMotorista().getId())) {
                log.warn("Manifesto {} já está gravado em outro motorista; corrigindo só a viagem existente",
                        numero);
            }
            return Correspondencia.de(viagem);
        }

        // Base antiga: escolhe a de menor id para o resultado não depender da ordem da lista.
        var semNumero = correspondentesPeloCriterioAntigo(motorista, data, valorFrete)
                .filter(v -> v.getNumeroManifesto() == null && v.getId() != null)
                .min(Comparator.comparing(Viagem::getId));
        if (semNumero.isEmpty()) return Correspondencia.NENHUMA;

        Viagem viagem = semNumero.get();
        // Condição IS NULL: se outra importação simultânea já deu número a esta
        // viagem, nada é alterado e a linha é recusada (409) em vez de duplicar.
        int marcadas = entityManager.createQuery(
                "UPDATE Viagem v SET v.numeroManifesto = :numero "
                + "WHERE v.id = :id AND v.numeroManifesto IS NULL")
                .setParameter("numero", numero)
                .setParameter("id", viagem.getId())
                .executeUpdate();
        if (marcadas == 0) {
            throw new DataIntegrityViolationException(
                    "Viagem " + viagem.getId() + " recebeu outro número de manifesto em paralelo");
        }
        viagem.setNumeroManifesto(numero);
        return Correspondencia.de(viagem);
    }

    private Stream<Viagem> correspondentesPeloCriterioAntigo(Motorista motorista, LocalDate data,
                                                            BigDecimal valorFrete) {
        return motorista.getViagens().stream()
                .filter(v -> data.equals(v.getData()) && valorFrete.compareTo(v.getValorFrete()) == 0);
    }

    private Veiculo resolverVeiculo(String placa) {
        if (!StringUtils.hasText(placa)) return null;
        // Mesma normalização do cruzamento e do seed: maiúsculas, sem hífen/espaços
        String placaNormalizada = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
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
     * Reimportação (D14): o CSV é a fonte do destino. Corrige quando veio um
     * destino e ele difere do gravado (inclusive "Não informado"); destino
     * vazio no CSV mantém o gravado. Usa UPDATE direto, e não só o setter,
     * porque o refresh feito na correção do pedágio descartaria uma alteração
     * ainda não gravada.
     */
    private void corrigirDestino(Viagem viagem, String destino) {
        if (destino == null || destino.equals(viagem.getDestino())) return;
        entityManager.createQuery("UPDATE Viagem v SET v.destino = :destino WHERE v.id = :id")
                .setParameter("destino", destino)
                .setParameter("id", viagem.getId())
                .executeUpdate();
        viagem.setDestino(destino);
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

