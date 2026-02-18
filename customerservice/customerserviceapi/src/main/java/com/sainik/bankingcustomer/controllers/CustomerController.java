package com.sainik.bankingcustomer.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sainik.bankingcustomer.dtos.CustomerDTO;
import com.sainik.bankingcustomer.dtos.GenericResponse;
import com.sainik.bankingcustomer.mappers.CustomerMapper;
import com.sainik.bankingcustomer.models.Customer;
import com.sainik.bankingcustomer.services.CustomerService;

@RestController
@RequestMapping("/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerMapper customerMapper;

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PostMapping("/v1.0")
    public ResponseEntity<GenericResponse<CustomerDTO>> addCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        Customer savedCustomer = customerService.addCustomer(customerDTO);
        CustomerDTO savedDTO = customerMapper.entitytodto(savedCustomer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.success("Customer created successfully", savedDTO));
    }

    @GetMapping("/v1.0")
    public ResponseEntity<GenericResponse<List<CustomerDTO>>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerDTO> customerDTOs = customerMapper.entitytolistdto(customers);
        return ResponseEntity.ok(GenericResponse.success("Customers retrieved successfully", customerDTOs));
    }

    @GetMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<CustomerDTO>> getCustomerById(@PathVariable("id") Long id) {
        Customer customer = customerService.getCustomerById(id);
        CustomerDTO customerDTO = customerMapper.entitytodto(customer);
        return ResponseEntity.ok(GenericResponse.success("Customer retrieved successfully", customerDTO));
    }

    @GetMapping("/v1.0/email/{email}")
    public ResponseEntity<GenericResponse<CustomerDTO>> getCustomerByEmail(@PathVariable("email") String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        CustomerDTO customerDTO = customerMapper.entitytodto(customer);
        return ResponseEntity.ok(GenericResponse.success("Customer retrieved successfully", customerDTO));
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PutMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<CustomerDTO>> updateCustomer(@PathVariable("id") Long id,
                                                                       @Valid @RequestBody CustomerDTO customerDTO) {
        Customer updatedCustomer = customerService.updateCustomer(id, customerDTO);
        CustomerDTO updatedDTO = customerMapper.entitytodto(updatedCustomer);
        return ResponseEntity.ok(GenericResponse.success("Customer updated successfully", updatedDTO));
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @DeleteMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<Object>> deleteCustomer(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(GenericResponse.success("Customer deleted successfully", null));
    }
}
