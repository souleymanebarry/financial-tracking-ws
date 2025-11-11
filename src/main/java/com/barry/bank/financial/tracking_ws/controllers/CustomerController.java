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

    /**
     *  Deletes a customer after sending their data to the archiving microservice,
     * including all associated accounts and operations.
     *
     * <p>This method performs the following steps:</p>
     *  <ol>
     *    <li>Retrieves the complete customer data with accounts and operations.</li>
     *    <li>Sends the customer data to the archiving microservice.</li>
     *    <li>Deletes the customer and all related accounts and operations.</li>
     *  </ol>
     *
     * @param customerId the unique identifier of the customer to archive and delete
     * @return a {@link ResponseEntity} indicating the result of the operation:
     *         <ul>
     *           <li>204 No Content if the customer was successfully archived and deleted</li>
     *           <li>404 Not Found if the customer does not exist</li>
     *         </ul>
     * @throws RuntimeException if an error occurs during the archiving process
     */
    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deleteCustomer(@PathVariable UUID customerId);

}
