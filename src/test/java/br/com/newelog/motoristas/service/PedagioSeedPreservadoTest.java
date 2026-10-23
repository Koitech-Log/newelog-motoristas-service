package br.com.newelog.motoristas.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import br.com.newelog.motoristas.dto.ManifestoCadastroDTO;
import br.com.newelog.motoristas.repository.ViagemRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedagioSeedPreservadoTest {
    @Autowired DataSource dataSource;
    @Autowired ViagemRepository viagens;
    @Autowired ManifestoCadastroService cadastro;
    @Autowired RentabilidadeAnaliseService dashboard;
    @Autowired EntityManager em;

    @Test
    void reprocessaAgostoInteiroSemAlterarNenhumPedagioNemDuplicar() throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/migration/V2__seed_dados_iniciais.sql")) {
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("(?m)^SELECT setval\\([^\\n]+$", "");
            // H2 não possui setval; somente esse realinhamento de sequences é
            // removido. Os 694 INSERTs originais são executados sem alterações.
            new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(dataSource);
        }
        Map<Long, BigDecimal> antes = new LinkedHashMap<>();
        var lote = viagens.findAll().stream().map(v -> {
            antes.put(v.getId(), v.getValorPedagio());
            var m = v.getMotorista();
            return new ManifestoCadastroDTO(m.getCodigoExterno(), v.getData(),
                m.getNome(), m.getCpfCnpj(), m.getVeiculo().getPlaca(),
                m.getStatus().name(), v.getValorFrete().doubleValue(), v.getValorPedagio());
        }).toList();
        assertThat(lote).hasSize(694);
        assertThat(dashboard.buscar(2026, 8).geral().custoTotal()).isEqualByComparingTo("48587.98");
        for (var dto : lote) cadastro.cadastrar(dto);
        em.flush();
        em.clear();
        Map<Long, BigDecimal> depois = new LinkedHashMap<>();
        viagens.findAll().forEach(v -> depois.put(v.getId(), v.getValorPedagio()));
        assertThat(depois).containsExactlyInAnyOrderEntriesOf(antes);
        assertThat(viagens.count()).isEqualTo(694);
        assertThat(dashboard.buscar(2026, 8).geral().custoTotal()).isEqualByComparingTo("48587.98");
        System.out.println("SEED_AGOSTO_PRESERVADO: 694 viagens, todos os pedagios intactos, custo=48587.98");
    }
}
