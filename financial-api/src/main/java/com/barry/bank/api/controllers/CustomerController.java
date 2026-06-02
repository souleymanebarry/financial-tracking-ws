package com.barry.bank.api.controllers;


import com.barry.bank.api.dtos.CustomerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.barry.bank.api.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CustomerDTO.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @GetMapping("/all")
    ResponseEntity<List<CustomerDTO>> getAllCustomers();

    @Operation(
            summary = "Get customer by ID",
            description = "Retrieves a single customer by their unique identifier."
    )
    @ApiResponse(responseCode = "200", description = "Customer found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/{customerId}")
    ResponseEntity<CustomerDTO> getCustomerById(
            @Parameter(description = "Unique identifier of the customer", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID customerId);

    @Operation(
            summary = "Get paginated customers",
            description = "Retrieves customers using pagination parameters (page, size)."
    )
    @ApiResponse(responseCode = "200", description = "Paginated customers retrieved successfully",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CustomerDTO.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @GetMapping
    ResponseEntity<List<CustomerDTO>> getCustomersPaginated(
            @Parameter(description = "Page number (0-based)", example = "0")
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of customers per page", example = "5")
            @Min(1) @RequestParam(defaultValue = "5") int size);

    @Operation(
            summary = "Create a new customer",
            description = "Creates a new customer in the system."
    )
    @ApiResponse(responseCode = "201", description = "Customer created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @PostMapping
    ResponseEntity<CustomerDTO> createCustomer(@Validated(OnCreate.class) @RequestBody CustomerDTO customerDTO);

    @Operation(
            summary = "Partially update a customer",
            description = "Updates only provided fields of an existing customer."
    )
    @ApiResponse(responseCode = "200", description = "Customer updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PatchMapping("/{customerId}")
    ResponseEntity<CustomerDTO> partiallyUpdateCustomer(
            @Parameter(description = "Unique identifier of the customer to update", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerDTO customerDTO);

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
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "500", description = "Error during archiving or deletion process")
    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Unique identifier of the customer to delete", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID customerId);

}
