package com.sainik.bankingcustomer.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger security configuration for the Customer Service.
 *
 * Registers two OAuth2 schemes mirroring Keycloak client registrations:
 *   - "keycloakAdmin"      – admin scope       (management / read-only)
 *   - "keycloakDeveloper"  – developer scope   (full CRUD)
 *
 * The issuer URL is read from {@code keycloak.issuer} so it stays in sync
 * with the JWT resource-server configuration without duplicating the realm path.
 */
@Configuration
public class OpenAPISecurityConfig {

    /** e.g. http://localhost:8080/realms/banking-realm */
    @Value("${keycloak.issuer}")
    private String issuer;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("keycloakAdmin",
                                oauthScheme("admin",     "Admin access"))
                        .addSecuritySchemes("keycloakDeveloper",
                                oauthScheme("developer", "Developer / full-CRUD access"))
                )
                .addSecurityItem(new SecurityRequirement().addList("keycloakAdmin"))
                .addSecurityItem(new SecurityRequirement().addList("keycloakDeveloper"))
                .info(new Info()
                        .title("Banking Customer Service")
                        .description("Customer Management API with OAuth2 Security")
                        .version("1.0"));
    }

    private SecurityScheme oauthScheme(String scope, String description) {
        String authUrl  = issuer + "/oauth2/authorize";
        String tokenUrl = issuer + "/oauth2/token";

        OAuthFlow code = new OAuthFlow()
                .authorizationUrl(authUrl)
                .tokenUrl(tokenUrl)
                .scopes(new Scopes().addString(scope, description));

        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(code));
    }
}
