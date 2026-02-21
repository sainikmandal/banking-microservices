package com.sainik.bankingcustomer.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * Disables Spring Data REST's automatic endpoint exposure so that only
 * the explicitly defined @RestController endpoints appear in Swagger UI.
 *
 * Without this, Spring Data REST would publish /customers, /accounts, etc.
 * with HATEOAS links that clash with (and confuse) the custom controllers.
 */
@Configuration
public class SwaggerConfiguration implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(
            RepositoryRestConfiguration config, CorsRegistry cors) {
        config.setExposeRepositoryMethodsByDefault(false);
    }
}
