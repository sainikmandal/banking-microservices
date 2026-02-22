package com.sainik.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Spring Authorization Server configuration.
 *
 * Replaces Keycloak entirely — no Docker, no external dependency.
 * Runs on port 9000 and registers with Eureka.
 *
 * Endpoints (standard Spring Authorization Server paths):
 *   Authorization:  http://localhost:9000/oauth2/authorize
 *   Token:          http://localhost:9000/oauth2/token
 *   JWKS:           http://localhost:9000/oauth2/jwks
 *   OIDC Discovery: http://localhost:9000/.well-known/openid-configuration
 *   OIDC UserInfo:  http://localhost:9000/userinfo
 *
 * Registered clients:
 *   - account-service-client      → Client Credentials + Auth Code (Swagger on :8081)
 *   - customer-service-client     → Client Credentials + Auth Code (Swagger on :8082)
 *   - transaction-service-client  → Client Credentials + Auth Code (Swagger on :8083)
 *   - banking-gateway-client      → Auth Code + PKCE, public (Swagger on :8765)
 */
@Configuration
@EnableWebSecurity
public class AuthServerConfig {

    // ── Client secrets (set in application.properties) ────────────────────────

    @Value("${auth.clients.account-service.secret:account-secret}")
    private String accountServiceSecret;

    @Value("${auth.clients.customer-service.secret:customer-secret}")
    private String customerServiceSecret;

    @Value("${auth.clients.transaction-service.secret:transaction-secret}")
    private String transactionServiceSecret;

    // ── Test user credentials ──────────────────────────────────────────────────

    @Value("${auth.test-user.username:testuser}")
    private String testUsername;

    @Value("${auth.test-user.password:Test@1234}")
    private String testPassword;

    // ── Security filter chains ─────────────────────────────────────────────────

    /**
     * Authorization Server security — handles /oauth2/**, /userinfo, /.well-known/**
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults()); // Enable OIDC (userinfo + discovery endpoints)

        http
                // CORS must be applied here so /oauth2/token allows cross-origin requests from Swagger UI
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                // The auth server also validates its own JWTs for the userinfo endpoint
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Default security — shows the login form for human users (Swagger UI flow).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    // ── CORS ───────────────────────────────────────────────────────────────────

    /**
     * Allows Swagger UI pages (running on :8081, :8082, :8083, :8765) to call
     * /oauth2/token for the authorization code exchange step.
     * Without this, browsers block the token fetch with "Failed to fetch".
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:8081",
                "http://localhost:8082",
                "http://localhost:8083",
                "http://localhost:8765"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false); // not needed for token endpoint

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Registered clients ─────────────────────────────────────────────────────

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder) {
        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .build();

        // Shared client settings for service clients (PKCE not required for backend clients)
        ClientSettings serviceClientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .build();

        // --- account-service-client ---
        // Client Credentials:   used by account-service backend to call other services (uses secret)
        // Auth Code + PKCE:     used by Swagger UI in browser (no secret — NONE method)
        RegisteredClient accountClient = RegisteredClient.withId("account-service-client-id")
                .clientId("account-service-client")
                .clientSecret(encoder.encode(accountServiceSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // allows PKCE-only browser flow
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8081/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("developer")
                .scope("admin")
                .clientSettings(serviceClientSettings)
                .tokenSettings(tokenSettings)
                .build();

        // --- customer-service-client ---
        RegisteredClient customerClient = RegisteredClient.withId("customer-service-client-id")
                .clientId("customer-service-client")
                .clientSecret(encoder.encode(customerServiceSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8082/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("developer")
                .scope("admin")
                .clientSettings(serviceClientSettings)
                .tokenSettings(tokenSettings)
                .build();

        // --- transaction-service-client ---
        RegisteredClient transactionClient = RegisteredClient.withId("transaction-service-client-id")
                .clientId("transaction-service-client")
                .clientSecret(encoder.encode(transactionServiceSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8083/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("developer")
                .scope("admin")
                .clientSettings(serviceClientSettings)
                .tokenSettings(tokenSettings)
                .build();

        // --- banking-gateway-client ---
        // Public client (no secret), Auth Code + PKCE only
        // Used by Swagger UI on http://localhost:8765 (unified gateway view)
        RegisteredClient gatewayClient = RegisteredClient.withId("banking-gateway-client-id")
                .clientId("banking-gateway-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // public client
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8765/swagger-ui/oauth2-redirect.html")
                .scope(OidcScopes.OPENID)
                .scope("developer")
                .scope("admin")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)              // enforce PKCE
                        .requireAuthorizationConsent(false) // no consent screen
                        .build())
                .tokenSettings(tokenSettings)
                .build();

        return new InMemoryRegisteredClientRepository(
                accountClient, customerClient, transactionClient, gatewayClient
        );
    }

    // ── Users ──────────────────────────────────────────────────────────────────

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails testUser = User.withUsername(testUsername)
                .password(encoder.encode(testPassword))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(testUser);
    }

    // ── Authorization Server settings ──────────────────────────────────────────

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }

    // ── JWK / JWT ──────────────────────────────────────────────────────────────

    /**
     * RSA key pair for signing JWTs.
     * NOTE: Generated fresh on every restart — tokens issued before a restart
     * will fail validation. For production, load from a keystore or secret manager.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // ── Password encoder ───────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
