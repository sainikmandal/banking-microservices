package com.sainik.bankingcustomer.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.sainik.bankingcustomer.dtos.CustomerDTO;
import com.sainik.bankingcustomer.models.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer dtotoentity(CustomerDTO customerDTO);

    CustomerDTO entitytodto(Customer customer);

    List<CustomerDTO> entitytolistdto(List<Customer> customers);
}
