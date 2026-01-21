package com.xiong.payment_gateway.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiong.payment_gateway.dto.RefundRequest;
import com.xiong.payment_gateway.models.Refund;
import com.xiong.payment_gateway.service.RefundService;

@RestController
@RequestMapping("/api/v1/refunds")
@Slf4j
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * Process a refund for a payment transaction
     * 
     * @param request the refund request containing transaction ID and refund amount
     * @return Refund details with status
     * @throws IllegalArgumentException if request validation fails
     * @throws ResourceNotFoundException if transaction not found
     * @throws PaymentGatewayException if refund processing fails
     */
    @PostMapping
    public ResponseEntity<Refund> createRefund(@Valid @RequestBody RefundRequest request) {
        log.info("Processing refund for transaction: {}", request.getTransactionId());
        
        Refund refund = refundService.processRefund(request);
        
        // Return 201 CREATED for successful refund creation
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(refund);
    }
}