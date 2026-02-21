package com.sainik.bankingcustomer.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sainik.bankingcustomer.dtos.CustomerDTO;
import com.sainik.bankingcustomer.exceptions.CustomerAlreadyExistsException;
import com.sainik.bankingcustomer.exceptions.CustomerNotFoundException;
import com.sainik.bankingcustomer.mappers.CustomerMapper;
import com.sainik.bankingcustomer.models.Customer;
import com.sainik.bankingcustomer.repositories.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    // Create
    public Customer addCustomer(CustomerDTO customerDTO) {
        customerRepository.findByEmail(customerDTO.getEmail())
                .ifPresent(existing -> {
                    throw new CustomerAlreadyExistsException(
                            "Customer with email " + customerDTO.getEmail() + " already exists");
                });

        Customer customer = customerMapper.dtotoentity(customerDTO);
        customer.setCreatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    // Read All
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Read One by ID
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    // Read One by Email
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
    }

    // Update (full update)
    public Customer updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer customer = getCustomerById(id);
        customer.setFirstName(customerDTO.getFirstName());
        customer.setLastName(customerDTO.getLastName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setAddress(customerDTO.getAddress());
        return customerRepository.save(customer);
    }

    // Delete
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }

    // Internal probe — called by other microservices (e.g. account-service) via REST client
    public boolean customerExists(Long id) {
        return customerRepository.existsById(id);
    }
}
