package com.sainik.bankingaccountapi.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "banking.vault") // Best practice to use a prefix
public class VaultConfiguration {

    private String mysqlusername;
    private String mysqlpassword;

}