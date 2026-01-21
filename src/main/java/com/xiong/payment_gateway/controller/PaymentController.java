package com.xiong.payment_gateway.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiong.payment_gateway.dto.ApiResponse;
import com.xiong.payment_gateway.dto.PaymentRequest;
import com.xiong.payment_gateway.dto.PaymentResponse;
import com.xiong.payment_gateway.models.PaymentTransaction;
import com.xiong.payment_gateway.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create a new payment transaction
     * 
     * @param request the payment request containing payment details
     * @return PaymentResponse with transaction details
     *         - 201 CREATED for new payments
     *         - 409 CONFLICT for duplicate requests (idempotent)
     * @throws IllegalArgumentException if request validation fails
     * @throws PaymentGatewayException if payment processing fails
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @Valid @RequestBody PaymentRequest request
    ) {
        log.info("Processing payment for merchant: {}", request.getMerchantId());
        
        ApiResponse<PaymentResponse> response = paymentService.processPayment(request);
        
        return ResponseEntity
            .status(response.getStatusCode())
            .body(response.getData());
    }

    /**
     * Retrieve a payment transaction by ID
     * 
     * @param transactionId the transaction ID to retrieve
     * @return the payment transaction details
     * @throws ResourceNotFoundException if transaction not found
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentTransaction> getPayment(
        @PathVariable("transactionId") String transactionId
    ) {
        log.info("Fetching transaction: {}", transactionId);
        
        PaymentTransaction transaction = paymentService.getTransaction(transactionId);
        
        return ResponseEntity.ok(transaction);
    }
}
