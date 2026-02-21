package com.sainik.banking.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive (WebFlux) security configuration for the API Gateway.
 *
 * JWT validation is performed at the gateway using the Keycloak JWKS endpoint
 * configured in {@code application.yml}:
 * <pre>
 *   spring.security.oauth2.resourceserver.jwt.jwk-set-uri
 * </pre>
 *
 * Public routes (Swagger UI, actuator, inter-service probes) are allowed without
 * a token so developers can browse the aggregated API docs at the gateway level.
 * All other traffic must carry a valid Bearer JWT.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Swagger / SpringDoc UI
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                // Gateway-proxied per-service doc paths
                                "/accountservice/v3/api-docs/**",
                                "/customerservice/v3/api-docs/**",
                                "/transactionservice/v3/api-docs/**"
                        ).permitAll()
                        // Actuator (health probes, Eureka, etc.)
                        .pathMatchers("/actuator/**").permitAll()
                        // Internal service-to-service probes pass-through without JWT
                        .pathMatchers("/accounts/exists/**", "/customers/exists/**").permitAll()
                        // Everything else requires a valid Keycloak JWT
                        .anyExchange().authenticated()
                )
                // Validate Bearer tokens using JWKS from Keycloak (reactive JWT decoder)
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
