package com.xiong.payment_gateway.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiong.payment_gateway.dto.RefundRequest;
import com.xiong.payment_gateway.service.RefundService;

@RestController
@RequestMapping("/api/v1/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<?> createRefund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(refundService.processRefund(request));
    }
}