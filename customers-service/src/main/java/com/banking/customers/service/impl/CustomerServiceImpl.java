package com.banking.customers.service.impl;

import com.banking.common.exception.DuplicateResourceException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.customers.dto.CustomerRequestDto;
import com.banking.customers.dto.CustomerResponseDto;
import com.banking.customers.entity.Customer;
import com.banking.customers.mapper.CustomerMapper;
import com.banking.customers.repository.CustomerRepository;
import com.banking.customers.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Customer", "email", dto.getEmail());
        }

        Customer customer = customerMapper.toEntity(dto);
        Customer saved = customerRepository.save(customer);
        log.info("Customer created: {} {}", saved.getFirstName(), saved.getLastName());

        return customerMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", email));
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        // Check email uniqueness if changed
        if (!customer.getEmail().equals(dto.getEmail()) && customerRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Customer", "email", dto.getEmail());
        }

        customerMapper.updateEntity(customer, dto);
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: {}", updated.getId());

        return customerMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        customerRepository.delete(customer);
        log.info("Customer deleted: {}", id);
    }
}
