package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.CustomerDTO;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "bankAccounts", ignore = true)
    Customer customerDtoToCustomer(CustomerDTO customerDTO);

    CustomerDTO customerToCustomerDto(Customer customer);

}
