package com.banking.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("accounts-service", r -> r
                        .path("/api/accounts/**")
                        .uri("lb://accounts-service"))
                .route("customers-service", r -> r
                        .path("/api/customers/**")
                        .uri("lb://customers-service"))
                .route("transactions-service", r -> r
                        .path("/api/transactions/**")
                        .uri("lb://transactions-service"))
                .build();
    }
}
