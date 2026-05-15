package com.barry.bank.financial.tracking_ws.controllers;

import com.barry.bank.financial.tracking_ws.dtos.CustomerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Customers",
        description = "Customer management APIs (CRUD + pagination + orchestration delete with archive service)"
)
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/v1/customers")
public interface CustomerController {

    @Operation(
            summary = "Get all customers",
            description = "Retrieves the full list of customers without pagination."
    )
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    @GetMapping("/all")
    ResponseEntity<List<CustomerDTO>> getAllCustomers();

    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves a single customer by their unique identifier."
    )
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/{customerId}")
    ResponseEntity<CustomerDTO> getCustomerById(@PathVariable UUID customerId);


    @Operation(
            summary = "Get paginated customers",
            description = "Retrieves customers using pagination parameters (page, size)."
    )
    @ApiResponse(responseCode = "200", description = "Paginated customers retrieved successfully")
    @GetMapping
    ResponseEntity<List<CustomerDTO>> getCustomersPaginated(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "5")  int size);


    @Operation(
            summary = "Create a new customer",
            description = "Creates a new customer in the system."
    )
    @ApiResponse(responseCode = "200", description = "Customer created successfully")
    @PostMapping
    ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerDTO customerDTO);


    @Operation(
            summary = "Partially update a customer",
            description = "Updates only provided fields of an existing customer."
    )
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PatchMapping("/{customerId}")
    ResponseEntity<CustomerDTO> partiallyUpdateCustomer(@PathVariable UUID customerId,
                                        @RequestBody CustomerDTO customerDTO);

    @Operation(
            summary = "Delete a customer (with archiving workflow)",
            description = """
                    Deletes a customer using a safe distributed workflow:
                    
                    1. Retrieve full customer data (accounts + operations)
                    2. Send data to financial-archive-service for archival
                    3. Delete customer and all related data
                    
                    ⚠️ This is a critical distributed operation requiring reliability.
                    """
    )
   @ApiResponse(responseCode = "204", description = "Customer archived and deleted successfully")
   @ApiResponse(responseCode = "404", description = "Customer not found")
   @ApiResponse(responseCode = "500", description = "Error during archiving or deletion process")
    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deleteCustomer(@PathVariable UUID customerId);

}
