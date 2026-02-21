package com.sainik.bankingcustomer.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Fetches a short-lived service account token from Keycloak using the
 * OAuth2 Client Credentials grant.
 *
 * Used by outbound REST clients to authenticate inter-service calls without
 * forwarding the end-user's token.
 *
 * The client-id / secret come from {@code application.properties}:
 * <pre>
 *   keycloak.token-url=http://localhost:8080/realms/banking-realm/protocol/openid-connect/token
 *   keycloak.client-id=customer-service-client
 *   keycloak.client-secret=&lt;secret&gt;
 * </pre>
 */
@Service
public class KeycloakTokenService {

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    /** Dedicated RestTemplate — kept separate from RestClient to avoid circular deps. */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns a fresh bearer token for this service.
     * Callers should NOT cache the result; Keycloak enforces short expiry.
     */
    public String getServiceToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials"
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Failed to fetch service token from Keycloak");
        }

        return (String) response.getBody().get("access_token");
    }
}
