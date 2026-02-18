package com.sainik.bankingtransaction.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DBConfiguration {

    @Autowired
    private VaultConfiguration vaultConfiguration;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(datasourceUrl);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(vaultConfiguration.getMysqlusername());
        dataSource.setPassword(vaultConfiguration.getMysqlpassword());
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        return dataSource;
    }
}
