package com.banking.customers.service;

import com.banking.customers.dto.CustomerRequestDto;
import com.banking.customers.dto.CustomerResponseDto;

import java.util.List;

public interface CustomerService {
    CustomerResponseDto createCustomer(CustomerRequestDto dto);
    CustomerResponseDto getCustomer(Long id);
    CustomerResponseDto getCustomerByEmail(String email);
    List<CustomerResponseDto> getAllCustomers();
    CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto);
    void deleteCustomer(Long id);
}
