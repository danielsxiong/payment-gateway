package com.xiong.payment_gateway.grpc.client;

import com.xiong.payment_gateway.grpc.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

/**
 * Example gRPC client for the Payment Service
 * 
 * This is a demonstration of how to use the gRPC PaymentService.
 * In production, use this pattern in your client applications.
 */
@Slf4j
public class PaymentGrpcClientExample {
    
    private final ManagedChannel channel;
    private final PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;
    
    public PaymentGrpcClientExample(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.paymentStub = PaymentServiceGrpc.newBlockingStub(channel);
    }
    
    /**
     * Create a new payment transaction
     */
    public void createPayment() {
        try {
            log.info("Creating payment via gRPC...");
            
            PaymentRequest request = PaymentRequest.newBuilder()
                    .setMerchantId("merchant_123")
                    .setAmount("99.99")
                    .setCurrency("USD")
                    .setIdempotencyKey("idempotent-key-" + System.nanoTime())
                    .setCustomerId("customer_456")
                    .setPaymentMethod("CREDIT_CARD")
                    .setWebhookUrl("https://webhook.example.com/payment")
                    .putMetadata("order_id", "order_789")
                    .putMetadata("description", "Product purchase")
                    .build();
            
            PaymentResponse response = paymentStub.createPayment(request);
            
            log.info("Payment created successfully!");
            log.info("Transaction ID: {}", response.getTransactionId());
            log.info("Status: {}", response.getStatus());
            log.info("Amount: {} {}", response.getAmount(), response.getCurrency());
            log.info("Message: {}", response.getMessage());
            
            // Fetch the payment details
            getPayment(response.getTransactionId());
            
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus(), e);
        }
    }
    
    /**
     * Get payment transaction details by ID
     */
    public void getPayment(String transactionId) {
        try {
            log.info("Fetching payment details for transaction: {}", transactionId);
            
            GetPaymentRequest request = GetPaymentRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .build();
            
            PaymentTransaction transaction = paymentStub.getPayment(request);
            
            log.info("Payment details retrieved:");
            log.info("  ID: {}", transaction.getId());
            log.info("  Merchant ID: {}", transaction.getMerchantId());
            log.info("  Amount: {} {}", transaction.getAmount(), transaction.getCurrency());
            log.info("  Status: {}", transaction.getStatus());
            log.info("  Payment Method: {}", transaction.getPaymentMethod());
            log.info("  Created At: {}", transaction.getCreatedAt());
            log.info("  Updated At: {}", transaction.getUpdatedAt());
            
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus(), e);
        }
    }
    
    public void shutdown() {
        channel.shutdown();
    }
    
    public static void main(String[] args) {
        PaymentGrpcClientExample client = new PaymentGrpcClientExample("localhost", 9090);
        try {
            client.createPayment();
        } finally {
            client.shutdown();
        }
    }
}
