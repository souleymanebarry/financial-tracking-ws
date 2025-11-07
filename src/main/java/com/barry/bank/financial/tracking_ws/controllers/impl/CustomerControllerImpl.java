package com.barry.bank.financial.tracking_ws.controllers.impl;

import com.barry.bank.financial.tracking_ws.controllers.CustomerController;
import com.barry.bank.financial.tracking_ws.dtos.CustomerDTO;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.mappers.CustomerMapper;
import com.barry.bank.financial.tracking_ws.services.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Log4j2
@RequiredArgsConstructor
public class CustomerControllerImpl implements CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @Override
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        log.info("GET /api/v1/customers/withoutPaginations");
        List<Customer> customers = customerService.getCustomersWithoutPagination();
        List<CustomerDTO> dtos = customers.stream().map(customerMapper::customerToCustomerDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<CustomerDTO> getCustomerById(UUID customerId) {
        log.info("GET api/v1/customers/{}: ", customerId);
        Customer customer = customerService.getCustomerById(customerId);
        CustomerDTO customerDTO = customerMapper.customerToCustomerDto(customer);
        return ResponseEntity.ok(customerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> createCustomer(CustomerDTO customerDTO) {
        log.info("POST /api/v1/customers -> {}", customerDTO);
        Customer customer = customerMapper.customerDtoToCustomer(customerDTO);
        Customer savedCustomer = customerService.createCustomer(customer);
        return ResponseEntity.ok(customerMapper.customerToCustomerDto(savedCustomer));
    }

    @Override
    public ResponseEntity<List<CustomerDTO>> getCustomersPaginated(int page, int size) {
        log.info("GET /api/v1/customers?page{}&size={}", page, size);
        List<Customer> customersPaginated = customerService.getCustomersPaginated(page, size);
        List<CustomerDTO> dtos = customersPaginated.stream().map(customerMapper::customerToCustomerDto).toList();
        return ResponseEntity.ok(dtos);
    }


    @Override
    public ResponseEntity<CustomerDTO> partiallyUpdateCustomer(UUID customerId, CustomerDTO customerDTO) {
        log.info("PATCH /api/v1/customers/{}", customerId);
        Customer customer = customerMapper.customerDtoToCustomer(customerDTO);
        Customer updatedCustomer = customerService.partiallyUpdateCustomer(customerId, customer);
        CustomerDTO customerDto = customerMapper.customerToCustomerDto(updatedCustomer);
        return ResponseEntity.ok(customerDto);
    }

    @Override
    public ResponseEntity<Void> deleteCustomer(UUID customerId) {
        log.info("DELETE /api/v1/customers/{}", customerId);
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
