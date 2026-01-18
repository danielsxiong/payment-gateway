package com.xiong.payment_gateway.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiong.payment_gateway.dto.PaymentRequest;
import com.xiong.payment_gateway.dto.PaymentResponse;
import com.xiong.payment_gateway.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getPayment(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getTransaction(transactionId));
    }
}
