package com.sainik.banking.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dynamically aggregates Swagger UI docs from all downstream banking services.
 *
 * Scans the gateway's route definitions for routes whose ID ends with "-docs"
 * (e.g. accountservice-docs, customerservice-docs, transactionservice-docs)
 * and registers a Swagger URL group for each one.
 *
 * This lets you browse all three service APIs from a single Swagger UI at:
 *   http://localhost:8765/swagger-ui.html
 *
 * The {@code springdoc.swagger-ui.urls} list in application.yml provides a
 * static fallback; these dynamic URLs are merged on top of it.
 */
@Configuration
public class GatewaySwaggerAggregatorConfig {

    /** Suffix convention used by gateway route IDs that proxy API-docs requests. */
    private static final String DOCS_ROUTE_SUFFIX = "-docs";

    /**
     * Registers a {@link SwaggerUrl} for every "-docs" route in the gateway.
     *
     * @param routeDefinitionLocator Spring Cloud Gateway route registry
     * @param swaggerUiConfig        SpringDoc Swagger UI configuration (mutated in-place)
     */
    @Bean
    @Lazy(false)
    public Set<SwaggerUrl> buildSwaggerUrls(
            RouteDefinitionLocator routeDefinitionLocator,
            SwaggerUiConfigProperties swaggerUiConfig) {

        Set<SwaggerUrl> urls = new HashSet<>();

        List<RouteDefinition> routes =
                routeDefinitionLocator.getRouteDefinitions().collectList().block();

        if (routes != null) {
            for (RouteDefinition route : routes) {
                String id = route.getId();
                if (id != null && id.endsWith(DOCS_ROUTE_SUFFIX)) {
                    // e.g. "accountservice-docs" → display name "accountservice", path from route
                    String serviceName = id.substring(0, id.length() - DOCS_ROUTE_SUFFIX.length());

                    // Build the API docs URL as it will be proxied through the gateway
                    String apiDocsUrl = "/" + serviceName + "/v3/api-docs";

                    SwaggerUrl swaggerUrl = new SwaggerUrl(
                            serviceName,       // group name shown in the Swagger UI drop-down
                            apiDocsUrl,        // URL fetched by Swagger UI
                            null               // display name (null → use group name)
                    );

                    urls.add(swaggerUrl);
                }
            }
        }

        if (swaggerUiConfig.getUrls() == null) {
            swaggerUiConfig.setUrls(new HashSet<>());
        }
        swaggerUiConfig.getUrls().addAll(urls);

        return urls;
    }
}
