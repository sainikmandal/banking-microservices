package com.sainik.bankingaccountapi.client;

import com.sainik.bankingaccountapi.dtos.GenericResponse;
import com.sainik.bankingaccountapi.security.KeycloakTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * REST client for the Customer Service.
 *
 * Used to verify that a customer exists before opening a new account.
 * Authenticates with a service-account token obtained from Keycloak
 * (client-credentials grant) so the call is independent of the user's session.
 *
 * The customer-service exposes {@code GET /customers/exists/{id}} as a
 * permit-all endpoint specifically for internal probes like this one.
 */
@Component
@RequiredArgsConstructor
public class CustomerServiceClient {

    private final KeycloakTokenService tokenService;

    @Value("${customer.service.url}")
    private String customerServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns {@code true} when the customer with the given id exists
     * in the customer-service.
     *
     * @param customerId the customer's primary key
     * @throws RuntimeException if the customer-service returns no body
     */
    @SuppressWarnings("rawtypes")
    public boolean customerExists(Long customerId) {
        String token = tokenService.getServiceToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<GenericResponse> response =
                restTemplate.exchange(
                        customerServiceUrl + "/customers/exists/" + customerId,
                        HttpMethod.GET,
                        entity,
                        GenericResponse.class
                );

        if (response.getBody() == null) {
            throw new RuntimeException("No response from customer-service");
        }

        return (Boolean) response.getBody().getData();
    }
}
