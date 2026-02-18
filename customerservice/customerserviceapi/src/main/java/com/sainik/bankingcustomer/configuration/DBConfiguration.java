package com.sainik.bankingcustomer.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VaultConfiguration.class)
public class DBConfiguration {

    @Autowired
    private VaultConfiguration vaultConfiguration;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(vaultConfiguration.getMysqlusername())
                .password(vaultConfiguration.getMysqlpassword())
                .driverClassName(driverClassName)
                .build();
    }
}
