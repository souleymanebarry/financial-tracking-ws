package com.barry.bank.api.mappers;


import com.barry.bank.api.dtos.CustomerDTO;
import com.barry.bank.domain.entities.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "bankAccounts", ignore = true)
    Customer customerDtoToCustomer(CustomerDTO customerDTO);

    CustomerDTO customerToCustomerDto(Customer customer);
}
