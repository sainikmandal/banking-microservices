package com.sainik.bankingcustomer.services;

import com.sainik.bankingcustomer.dtos.CustomerDTO;
import com.sainik.bankingcustomer.exceptions.CustomerAlreadyExistsException;
import com.sainik.bankingcustomer.exceptions.CustomerNotFoundException;
import com.sainik.bankingcustomer.mappers.CustomerMapper;
import com.sainik.bankingcustomer.models.Customer;
import com.sainik.bankingcustomer.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerDTO customerDTO;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("Jordan");
        customer.setLastName("Lee");
        customer.setEmail("jordan.lee@bank.com");
        customer.setPhone("+31690000001");
        customer.setAddress("10 Bank Avenue, Rotterdam");
        customer.setCreatedAt(LocalDateTime.now());

        customerDTO = new CustomerDTO();
        customerDTO.setFirstName("Jordan");
        customerDTO.setLastName("Lee");
        customerDTO.setEmail("jordan.lee@bank.com");
        customerDTO.setPhone("+31690000001");
        customerDTO.setAddress("10 Bank Avenue, Rotterdam");
    }

    // ─── addCustomer ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addCustomer: should create and return customer when email is unique")
    void addCustomer_success() {
        when(customerRepository.findByEmail("jordan.lee@bank.com")).thenReturn(Optional.empty());
        when(customerMapper.dtotoentity(customerDTO)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.addCustomer(customerDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("jordan.lee@bank.com");
        assertThat(result.getFirstName()).isEqualTo("Jordan");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("addCustomer: should throw CustomerAlreadyExistsException when email is already registered")
    void addCustomer_duplicateEmail_throwsException() {
        when(customerRepository.findByEmail("jordan.lee@bank.com")).thenReturn(Optional.of(customer));

        assertThrows(CustomerAlreadyExistsException.class,
                () -> customerService.addCustomer(customerDTO));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("addCustomer: should set createdAt timestamp on new customer")
    void addCustomer_setsCreatedAt() {
        Customer unsaved = new Customer(); // no createdAt yet
        unsaved.setFirstName("Jordan");
        unsaved.setEmail("jordan.lee@bank.com");

        when(customerRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(customerMapper.dtotoentity(customerDTO)).thenReturn(unsaved);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.addCustomer(customerDTO);

        assertThat(result.getCreatedAt()).isNotNull();
    }

    // ─── getAllCustomers ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCustomers: should return all customers from repository")
    void getAllCustomers_returnsAll() {
        Customer second = new Customer();
        second.setId(2L);
        second.setEmail("second@bank.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer, second));

        List<Customer> result = customerService.getAllCustomers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("jordan.lee@bank.com");
        verify(customerRepository).findAll();
    }

    @Test
    @DisplayName("getAllCustomers: should return empty list when no customers exist")
    void getAllCustomers_emptyRepository_returnsEmptyList() {
        when(customerRepository.findAll()).thenReturn(List.of());

        List<Customer> result = customerService.getAllCustomers();

        assertThat(result).isEmpty();
    }

    // ─── getCustomerById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerById: should return customer when ID exists")
    void getCustomerById_found() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFirstName()).isEqualTo("Jordan");
    }

    @Test
    @DisplayName("getCustomerById: should throw CustomerNotFoundException when ID does not exist")
    void getCustomerById_notFound_throwsException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        CustomerNotFoundException ex = assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomerById(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    // ─── getCustomerByEmail ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getCustomerByEmail: should return customer when email exists")
    void getCustomerByEmail_found() {
        when(customerRepository.findByEmail("jordan.lee@bank.com")).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerByEmail("jordan.lee@bank.com");

        assertThat(result.getEmail()).isEqualTo("jordan.lee@bank.com");
    }

    @Test
    @DisplayName("getCustomerByEmail: should throw CustomerNotFoundException when email not found")
    void getCustomerByEmail_notFound_throwsException() {
        when(customerRepository.findByEmail("ghost@bank.com")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomerByEmail("ghost@bank.com"));
    }

    // ─── updateCustomer ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCustomer: should update all mutable fields and save")
    void updateCustomer_success() {
        CustomerDTO updateDTO = new CustomerDTO();
        updateDTO.setFirstName("Alex");
        updateDTO.setLastName("Smith");
        updateDTO.setEmail("alex.smith@bank.com");
        updateDTO.setPhone("+31690000099");
        updateDTO.setAddress("99 New Street, Amsterdam");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.updateCustomer(1L, updateDTO);

        assertThat(result.getFirstName()).isEqualTo("Alex");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getEmail()).isEqualTo("alex.smith@bank.com");
        assertThat(result.getPhone()).isEqualTo("+31690000099");
        assertThat(result.getAddress()).isEqualTo("99 New Street, Amsterdam");
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("updateCustomer: should throw CustomerNotFoundException when customer does not exist")
    void updateCustomer_notFound_throwsException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.updateCustomer(99L, customerDTO));

        verify(customerRepository, never()).save(any());
    }

    // ─── deleteCustomer ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCustomer: should delete customer when ID exists")
    void deleteCustomer_success() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    @DisplayName("deleteCustomer: should throw CustomerNotFoundException when ID does not exist")
    void deleteCustomer_notFound_throwsException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.deleteCustomer(99L));

        verify(customerRepository, never()).delete(any());
    }
}
