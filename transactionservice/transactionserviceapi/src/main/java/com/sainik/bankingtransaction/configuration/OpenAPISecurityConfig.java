package com.sainik.bankingtransaction.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transaction Service API",
                version = "1.0",
                description = "Banking Transaction Management Service"
        )
)
@SecurityScheme(
        name = "oauth2",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "http://localhost:8080/realms/banking-realm/protocol/openid-connect/auth",
                        tokenUrl = "http://localhost:8080/realms/banking-realm/protocol/openid-connect/token",
                        scopes = {
                                @OAuthScope(name = "openid", description = "OpenID Connect scope"),
                                @OAuthScope(name = "developer", description = "Developer scope for write operations")
                        }
                )
        )
)
public class OpenAPISecurityConfig {
}
