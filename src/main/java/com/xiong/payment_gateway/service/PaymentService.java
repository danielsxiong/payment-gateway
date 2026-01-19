package com.xiong.payment_gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiong.payment_gateway.dto.PaymentRequest;
import com.xiong.payment_gateway.dto.PaymentResponse;
import com.xiong.payment_gateway.models.PaymentTransaction;
import com.xiong.payment_gateway.models.TransactionStatus;
import com.xiong.payment_gateway.repository.PaymentRepository;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final WebhookService webhookService;

    public PaymentService(
        PaymentRepository paymentRepository,
        IdempotencyService idempotencyService,
        WebhookService webhookService
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.webhookService = webhookService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Check idempotency
        if (idempotencyService.isProcessed(request.getIdempotencyKey())) {
            String existingTxnId = idempotencyService.getTransactionId(
                request.getIdempotencyKey()
            );
            PaymentTransaction existing = paymentRepository.findById(existingTxnId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            log.info("Returning existing transaction: {}", existingTxnId);
            return buildResponse(existing, "Duplicate request - returning existing transaction");
        }

        // Create transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setMerchantId(request.getMerchantId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setCustomerId(request.getCustomerId());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setMetadata(request.getMetadata());
        transaction.setStatus(TransactionStatus.PROCESSING);

        // Save transaction
        transaction = paymentRepository.save(transaction);
        
        // Mark as processed in Redis
        idempotencyService.markAsProcessed(
            request.getIdempotencyKey(),
            transaction.getId()
        );

        // Simulate payment processing
        boolean success = processWithPaymentProvider(transaction);
        
        if (success) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            log.info("Payment successful: {}", transaction.getId());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("Payment failed: {}", transaction.getId());
        }
        
        transaction = paymentRepository.save(transaction);

        // Send webhook asynchronously
        webhookService.sendWebhook(transaction, request.getWebhookUrl());

        return buildResponse(transaction, "Payment processed successfully");
    }

    private boolean processWithPaymentProvider(PaymentTransaction transaction) {
        // Mock payment processing - replace with actual payment provider integration
        // For demo: succeed 90% of the time
        return Math.random() < 0.9;
    }

    private PaymentResponse buildResponse(PaymentTransaction txn, String message) {
        return PaymentResponse.builder()
            .transactionId(txn.getId())
            .status(txn.getStatus().toString())
            .amount(txn.getAmount())
            .currency(txn.getCurrency())
            .createdAt(txn.getCreatedAt())
            .message(message)
            .build();
    }

    public PaymentTransaction getTransaction(String transactionId) {
        return paymentRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}