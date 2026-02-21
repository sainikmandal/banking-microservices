package com.sainik.bankingtransaction.client;

import com.sainik.bankingtransaction.security.KeycloakTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * REST client for the Account Service using Spring's RestClient (not RestTemplate).
 *
 * Calls {@code GET /accounts/exists/{id}} — a permit-all probe endpoint — to
 * verify that an account exists before a transaction is recorded.
 *
 * Uses a service-account token (client-credentials grant) so the call is
 * independent of the end-user's session.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestClient restClient;
    private final KeycloakTokenService tokenService;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    /**
     * Returns {@code true} when the account with the given id exists in account-service.
     * Returns {@code false} on any error (network failure, 4xx, 5xx) so the
     * transaction can be recorded as FAILED rather than throwing an unhandled exception.
     */
    @SuppressWarnings("unchecked")
    public boolean accountExists(Long accountId) {
        try {
            String token = tokenService.getServiceToken();

            Map<String, Object> body = restClient.get()
                    .uri(accountServiceUrl + "/accounts/exists/" + accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                log.warn("account-service returned null body for accountId={}", accountId);
                return false;
            }

            Object data = body.get("data");
            return Boolean.TRUE.equals(data);

        } catch (Exception ex) {
            log.error("Failed to reach account-service for accountId={}: {}", accountId, ex.getMessage());
            return false;
        }
    }
}
