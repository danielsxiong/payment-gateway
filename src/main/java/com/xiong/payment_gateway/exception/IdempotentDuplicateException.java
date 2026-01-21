package com.xiong.payment_gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request is detected as a duplicate based on idempotency key.
 * These requests should return 409 CONFLICT with the existing transaction.
 */
public class IdempotentDuplicateException extends PaymentGatewayException {
    private final String transactionId;

    public IdempotentDuplicateException(String transactionId, String message) {
        super(message, HttpStatus.CONFLICT, "IDEMPOTENT_DUPLICATE");
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
