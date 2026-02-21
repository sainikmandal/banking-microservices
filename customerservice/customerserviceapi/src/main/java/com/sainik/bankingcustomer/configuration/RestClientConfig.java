package com.sainik.bankingcustomer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides a shared {@link RestClient} bean used for outbound HTTP calls.
 *
 * A single shared builder keeps connection pool settings in one place
 * and makes it easy to add interceptors later (logging, tracing, etc.).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
