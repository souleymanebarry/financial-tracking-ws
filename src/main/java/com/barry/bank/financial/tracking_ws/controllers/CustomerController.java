package com.barry.bank.financial.tracking_ws.controllers;

import com.barry.bank.financial.tracking_ws.dtos.CustomerDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/v1/customers")
public interface CustomerController {

    @GetMapping("/all")
    ResponseEntity<List<CustomerDTO>> getAllCustomers();

    @GetMapping("/{customerId}")
    ResponseEntity<CustomerDTO> getCustomerById(@PathVariable UUID customerId);

    @GetMapping
    ResponseEntity<List<CustomerDTO>> getCustomersPaginated(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "5")  int size);

    @PostMapping
    ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO);

    @PatchMapping("/{customerId}")
    ResponseEntity<CustomerDTO> partiallyUpdateCustomer(@PathVariable UUID customerId,
                                        @RequestBody CustomerDTO customerDTO);

    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deleteCustomer(@PathVariable UUID customerId);

}
