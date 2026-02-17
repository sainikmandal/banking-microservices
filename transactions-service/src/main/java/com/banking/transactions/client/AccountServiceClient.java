package com.banking.transactions.client;

import com.banking.common.dto.ApiResponseDto;
import com.banking.transactions.dto.AccountResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "accounts-service", fallback = AccountServiceFallback.class)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    ApiResponseDto<AccountResponseDto> getAccount(@PathVariable("id") Long id);

    @PutMapping("/api/accounts/{id}/balance")
    ApiResponseDto<AccountResponseDto> updateBalance(@PathVariable("id") Long id, @RequestBody Map<String, Object> body);
}
