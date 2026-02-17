package com.banking.transactions.client;

import com.banking.common.dto.ApiResponseDto;
import com.banking.common.exception.BusinessException;
import com.banking.transactions.dto.AccountResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AccountServiceFallback implements AccountServiceClient {

    @Override
    public ApiResponseDto<AccountResponseDto> getAccount(Long id) {
        log.error("Account service unavailable - getAccount fallback for id: {}", id);
        throw new BusinessException("Account service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ApiResponseDto<AccountResponseDto> updateBalance(Long id, Map<String, Object> body) {
        log.error("Account service unavailable - updateBalance fallback for id: {}", id);
        throw new BusinessException("Account service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
