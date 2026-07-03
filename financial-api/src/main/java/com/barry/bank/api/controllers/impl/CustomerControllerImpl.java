package com.barry.bank.api.controllers.impl;

import com.barry.bank.api.controllers.CustomerController;
import com.barry.bank.api.customer.service.CustomerDeletionService;
import com.barry.bank.api.dtos.CustomerDTO;
import com.barry.bank.api.mappers.CustomerMapper;
import com.barry.bank.application.services.CustomerService;
import com.barry.bank.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@Log4j2
@RequiredArgsConstructor
public class CustomerControllerImpl implements CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private final CustomerDeletionService customerDeletionService;

    @Override
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        log.debug("GET /api/v1/customers/all");
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerDTO> dtos = customers.stream().map(customerMapper::customerToCustomerDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<CustomerDTO> getCustomerById(UUID customerId) {
        log.debug("GET /api/v1/customers/{}", customerId);
        Customer customer = customerService.getCustomerById(customerId);
        CustomerDTO customerDTO = customerMapper.customerToCustomerDto(customer);
        return ResponseEntity.ok(customerDTO);
    }

    @Override
    public ResponseEntity<CustomerDTO> createCustomer(CustomerDTO customerDTO) {
        log.info("POST /api/v1/customers");
        Customer customer = customerMapper.customerDtoToCustomer(customerDTO);
        Customer savedCustomer = customerService.createCustomer(customer);
        CustomerDTO savedDTO = customerMapper.customerToCustomerDto(savedCustomer);
        URI location = URI.create(String.format("api/v1/customers/%s", savedCustomer.getCustomerId()));
        return ResponseEntity.created(location).body(savedDTO);
    }

    @Override
    public ResponseEntity<List<CustomerDTO>> getCustomersPaginated(int page, int size) {
        log.debug("GET /api/v1/customers?page={}&size={}", page, size);
        List<Customer> customersPaginated = customerService.getCustomers(page, size);
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
        customerDeletionService.archiveAndDeleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
