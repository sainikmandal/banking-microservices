package com.sainik.bankingcustomer.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPISecurityConfig {

    @Value("${keycloak.auth-server-url}")
    String authServerUrl;

    @Value("${keycloak.realm}")
    String realm;

    @Bean
    public OpenAPI openAPI() {
        String authUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/auth";
        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("keycloak", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Oauth2 flow")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(new Scopes()
                                                        .addString("openid", "openid scope")
                                                        .addString("profile", "profile scope")
                                                        .addString("developer", "developer scope"))))))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"))
                .info(new Info()
                        .title("Banking Customer Service")
                        .description("Customer Management API with OAuth2 Security")
                        .version("1.0"));
    }
}
