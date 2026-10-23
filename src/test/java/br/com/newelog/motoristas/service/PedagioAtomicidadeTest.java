package br.com.newelog.motoristas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.repository.MotoristaRepository;
import br.com.newelog.motoristas.repository.VeiculoRepository;
import br.com.newelog.motoristas.repository.ViagemRepository;

@SpringBootTest
@ActiveProfiles("test")
class PedagioAtomicidadeTest {
    @Autowired ManifestoCadastroService cadastro;
    @Autowired MotoristaRepository motoristas;
    @Autowired VeiculoRepository veiculos;
    @Autowired ViagemRepository viagens;
    @Autowired PlatformTransactionManager transactionManager;
    Long motoristaId;

    private ManifestoCadastroDTO dados(String valor) {
        return new ManifestoCadastroDTO("CONCORRENCIA-PEDAGIO", LocalDate.of(2026, 7, 1),
            "Concorrencia Teste", "11144477735", "ZZZ1Z23", "Finalizado", 1000.00,
            valor == null ? null : new BigDecimal(valor));
    }
    private BigDecimal custo() {
        return new TransactionTemplate(transactionManager).execute(s ->
            motoristas.findById(motoristaId).orElseThrow().getViagens().get(0).getValorPedagio());
    }
    @BeforeEach
    void preparar() { motoristaId = cadastro.cadastrar(dados("0.00")).id(); }
    @AfterEach
    void limparSomenteFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            motoristas.findById(motoristaId).ifPresent(motoristas::delete);
            motoristas.flush();
            veiculos.findByPlaca("ZZZ1Z23").ifPresent(veiculos::delete);
        });
    }

    @Test
    void rollbackNaoDeixaCorrecaoParcial() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(s -> {
            cadastro.cadastrar(dados("165.87"));
            throw new IllegalStateException("falha simulada");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(custo()).isEqualByComparingTo("0.00");
    }

    @Test
    void duasCorrecoesConcorrentesNaoSobrescrevemOPrimeiroValorConfirmado() throws Exception {
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { prontas.countDown(); iniciar.await(); cadastro.cadastrar(dados("165.87")); return true; });
            var b = pool.submit(() -> { prontas.countDown(); iniciar.await(); cadastro.cadastrar(dados("200.00")); return true; });
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(b.get(20, TimeUnit.SECONDS)).isTrue();
        }
        BigDecimal confirmado = custo();
        assertThat(confirmado).isIn(new BigDecimal("165.87"), new BigDecimal("200.00"));
        cadastro.cadastrar(dados("300.00"));
        assertThat(custo()).isEqualByComparingTo(confirmado);
        new TransactionTemplate(transactionManager).executeWithoutResult(s ->
            assertThat(motoristas.findById(motoristaId).orElseThrow().getViagens()).hasSize(1));
    }
}
