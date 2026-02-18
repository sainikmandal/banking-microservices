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

    // POST - Register a new customer
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PostMapping("/v1.0")
    public ResponseEntity<GenericResponse> addCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        Customer savedCustomer = customerService.addCustomer(customerDTO);
        CustomerDTO savedDTO = customerMapper.entitytodto(savedCustomer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponse(savedDTO));
    }

    // GET - View all customer profiles
    @GetMapping("/v1.0")
    public ResponseEntity<GenericResponse> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerDTO> customerDTOs = customerMapper.entitytolistdto(customers);
        return ResponseEntity.ok(new GenericResponse(customerDTOs));
    }

    // GET - View customer profile by ID
    @GetMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> getCustomerById(@PathVariable("id") Long id) {
        Customer customer = customerService.getCustomerById(id);
        CustomerDTO customerDTO = customerMapper.entitytodto(customer);
        return ResponseEntity.ok(new GenericResponse(customerDTO));
    }

    // GET - View customer profile by email
    @GetMapping("/v1.0/email/{email}")
    public ResponseEntity<GenericResponse> getCustomerByEmail(@PathVariable("email") String email) {
        Customer customer = customerService.getCustomerByEmail(email);
        CustomerDTO customerDTO = customerMapper.entitytodto(customer);
        return ResponseEntity.ok(new GenericResponse(customerDTO));
    }

    // PUT - Update customer data
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PutMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> updateCustomer(@PathVariable("id") Long id,
                                                          @Valid @RequestBody CustomerDTO customerDTO) {
        Customer updatedCustomer = customerService.updateCustomer(id, customerDTO);
        CustomerDTO updatedDTO = customerMapper.entitytodto(updatedCustomer);
        return ResponseEntity.ok(new GenericResponse(updatedDTO));
    }

    // DELETE - Remove a customer profile
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @DeleteMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> deleteCustomer(@PathVariable("id") Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(new GenericResponse("Customer deleted successfully"));
    }
}
