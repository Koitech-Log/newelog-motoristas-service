package br.com.newelog.motoristas.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String EMISSOR = "newelog-auth-service";

    private static final String ORIGEM_PRODUCAO =
        "https://newelog.vercel.app";

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {

        RespostaErroSeguranca erros = new RespostaErroSeguranca();

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Permite preflight CORS do navegador.
                .requestMatchers(
                    HttpMethod.OPTIONS, "/**"
                ).permitAll()

                // Endpoints públicos de monitoramento.
                .requestMatchers(
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                // Todas as demais rotas exigem JWT válido,
                // independentemente do perfil do usuário.
                .anyRequest().authenticated()
            )

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros)
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(conversor())
                )
                .authenticationEntryPoint(erros)
                .accessDeniedHandler(erros)
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.jwt.secret}") String segredo
    ) {

        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET precisa ter pelo menos 32 caracteres."
            );
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(
                new SecretKeySpec(bytes, "HmacSHA256")
            )
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(EMISSOR)
        );

        return decoder;
    }

    private JwtAuthenticationConverter conversor() {

        JwtGrantedAuthoritiesConverter autoridades =
            new JwtGrantedAuthoritiesConverter();

        autoridades.setAuthoritiesClaimName("perfil");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor =
            new JwtAuthenticationConverter();

        conversor.setJwtGrantedAuthoritiesConverter(
            autoridades
        );

        return conversor;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-url:http://localhost:5173}")
            String frontendUrl
    ) {

        Set<String> origens = new LinkedHashSet<>();

        origens.add(ORIGEM_PRODUCAO);

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .forEach(origens::add);
        }

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(new ArrayList<>(origens));

        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
