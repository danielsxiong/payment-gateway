package com.xiong.payment_gateway.grpc.client;

import com.xiong.payment_gateway.grpc.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

/**
 * Example gRPC client for the Refund Service
 * 
 * This is a demonstration of how to use the gRPC RefundService.
 * In production, use this pattern in your client applications.
 */
@Slf4j
public class RefundGrpcClientExample {
    
    private final ManagedChannel channel;
    private final RefundServiceGrpc.RefundServiceBlockingStub refundStub;
    
    public RefundGrpcClientExample(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.refundStub = RefundServiceGrpc.newBlockingStub(channel);
    }
    
    /**
     * Process a refund for a payment transaction
     */
    public void createRefund(String transactionId, String amount, String reason) {
        try {
            log.info("Processing refund for transaction: {}", transactionId);
            
            RefundRequest request = RefundRequest.newBuilder()
                    .setTransactionId(transactionId)
                    .setAmount(amount)
                    .setReason(reason)
                    .build();
            
            Refund response = refundStub.createRefund(request);
            
            log.info("Refund processed successfully!");
            log.info("Refund ID: {}", response.getId());
            log.info("Transaction ID: {}", response.getTransactionId());
            log.info("Amount: {}", response.getAmount());
            log.info("Status: {}", response.getStatus());
            log.info("Reason: {}", response.getReason());
            log.info("Created At: {}", response.getCreatedAt());
            
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus(), e);
        }
    }
    
    public void shutdown() {
        channel.shutdown();
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java RefundGrpcClientExample <transactionId> [amount] [reason]");
            System.exit(1);
        }
        
        String transactionId = args[0];
        String amount = args.length > 1 ? args[1] : "50.00";
        String reason = args.length > 2 ? args[2] : "Customer request";
        
        RefundGrpcClientExample client = new RefundGrpcClientExample("localhost", 9090);
        try {
            client.createRefund(transactionId, amount, reason);
        } finally {
            client.shutdown();
        }
    }
}
