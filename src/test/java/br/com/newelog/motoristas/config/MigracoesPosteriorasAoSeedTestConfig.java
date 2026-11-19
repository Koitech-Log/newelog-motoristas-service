package br.com.newelog.motoristas.config;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Os testes usam H2 com spring.flyway.target=1, porque a V2 (seed) usa setval,
 * que o H2 não tem. Depois do Flyway, esta estratégia executa as migrations de
 * schema posteriores à V2 (V3 em diante), lidas dos MESMOS arquivos de
 * produção, para o schema dos testes acompanhar o real. O banco H2 em memória
 * é compartilhado pelos contextos de teste da mesma JVM, então cada script é
 * registrado e roda uma vez só.
 */
@Configuration
@Profile("test")
class MigracoesPosteriorasAoSeedTestConfig {

    private static final Pattern VERSAO = Pattern.compile("^V(\\d+)__.*\\.sql$");
    private static final int ULTIMA_VERSAO_PULADA = 2;

    @Bean
    FlywayMigrationStrategy aplicarMigracoesPosterioresAoSeed(DataSource dataSource) {
        return flyway -> {
            flyway.migrate();
            try (Connection conexao = dataSource.getConnection()) {
                var jdbc = new JdbcTemplate(new SingleConnectionDataSource(conexao, true));
                jdbc.execute("CREATE TABLE IF NOT EXISTS teste_migracao_aplicada (versao INT PRIMARY KEY)");
                Resource[] scripts = new PathMatchingResourcePatternResolver()
                        .getResources("classpath:db/migration/V*__*.sql");
                Arrays.stream(scripts)
                        .filter(r -> versao(r) > ULTIMA_VERSAO_PULADA)
                        .sorted(Comparator.comparingInt(MigracoesPosteriorasAoSeedTestConfig::versao))
                        .filter(r -> jdbc.queryForObject(
                                "SELECT COUNT(*) FROM teste_migracao_aplicada WHERE versao = ?",
                                Integer.class, versao(r)) == 0)
                        .forEach(r -> {
                            ScriptUtils.executeSqlScript(conexao, r);
                            jdbc.update("INSERT INTO teste_migracao_aplicada (versao) VALUES (?)", versao(r));
                        });
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao aplicar migrations posteriores ao seed no H2", e);
            }
        };
    }

    private static int versao(Resource script) {
        Matcher m = VERSAO.matcher(script.getFilename() == null ? "" : script.getFilename());
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }
}
