package com.xiong.payment_gateway.grpc;

import com.google.protobuf.Timestamp;
import com.xiong.payment_gateway.dto.ApiResponse;
import com.xiong.payment_gateway.dto.PaymentRequest;
import com.xiong.payment_gateway.dto.PaymentResponse;
import com.xiong.payment_gateway.models.PaymentTransaction;
import com.xiong.payment_gateway.service.PaymentService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@GrpcService
@Slf4j
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {
    
    private final PaymentService paymentService;
    
    public PaymentGrpcService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @Override
    public void createPayment(
            com.xiong.payment_gateway.grpc.PaymentRequest request,
            StreamObserver<com.xiong.payment_gateway.grpc.PaymentResponse> responseObserver) {
        try {
            log.info("gRPC: Processing payment for merchant: {}", request.getMerchantId());
            
            // Convert proto request to DTO
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setMerchantId(request.getMerchantId());
            paymentRequest.setAmount(new BigDecimal(request.getAmount()));
            paymentRequest.setCurrency(request.getCurrency());
            paymentRequest.setIdempotencyKey(request.getIdempotencyKey());
            paymentRequest.setCustomerId(request.getCustomerId());
            paymentRequest.setPaymentMethod(request.getPaymentMethod());
            paymentRequest.setWebhookUrl(request.getWebhookUrl());
            
            // Convert metadata map
            Map<String, Object> metadata = new HashMap<>(request.getMetadataMap());
            paymentRequest.setMetadata(metadata);
            
            // Process payment using existing service
            ApiResponse<PaymentResponse> apiResponse = paymentService.processPayment(paymentRequest);
            PaymentResponse paymentResponse = apiResponse.getData();
            
            // Convert DTO to proto response
            com.xiong.payment_gateway.grpc.PaymentResponse protoResponse = com.xiong.payment_gateway.grpc.PaymentResponse.newBuilder()
                    .setTransactionId(paymentResponse.getTransactionId())
                    .setStatus(paymentResponse.getStatus())
                    .setAmount(paymentResponse.getAmount().toString())
                    .setCurrency(paymentResponse.getCurrency())
                    .setCreatedAt(localDateTimeToTimestamp(paymentResponse.getCreatedAt()))
                    .setMessage(paymentResponse.getMessage() != null ? paymentResponse.getMessage() : "")
                    .build();
            
            responseObserver.onNext(protoResponse);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error processing payment via gRPC", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getPayment(
            GetPaymentRequest request,
            StreamObserver<com.xiong.payment_gateway.grpc.PaymentTransaction> responseObserver) {
        try {
            log.info("gRPC: Fetching transaction: {}", request.getTransactionId());
            
            // Fetch transaction using existing service
            com.xiong.payment_gateway.models.PaymentTransaction transaction =
                    paymentService.getTransaction(request.getTransactionId());
            
            // Convert model to proto response
            com.xiong.payment_gateway.grpc.PaymentTransaction protoTransaction = convertToProtoTransaction(transaction);
            
            responseObserver.onNext(protoTransaction);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error fetching payment via gRPC", e);
            responseObserver.onError(e);
        }
    }
    
    private com.xiong.payment_gateway.grpc.PaymentTransaction convertToProtoTransaction(
            com.xiong.payment_gateway.models.PaymentTransaction transaction) {
        Map<String, String> metadataMap = new HashMap<>();
        transaction.getMetadata().forEach((key, value) ->
            metadataMap.put(key, value != null ? value.toString() : "")
        );

        return com.xiong.payment_gateway.grpc.PaymentTransaction.newBuilder()
                .setId(transaction.getId())
                .setMerchantId(transaction.getMerchantId())
                .setAmount(transaction.getAmount().toString())
                .setCurrency(transaction.getCurrency())
                .setStatus(transaction.getStatus().toString())
                .setIdempotencyKey(transaction.getIdempotencyKey())
                .setCustomerId(transaction.getCustomerId() != null ? transaction.getCustomerId() : "")
                .setPaymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod() : "")
                .putAllMetadata(metadataMap)
                .setCreatedAt(localDateTimeToTimestamp(transaction.getCreatedAt()))
                .setUpdatedAt(localDateTimeToTimestamp(transaction.getUpdatedAt()))
                .build();
    }
    
    private Timestamp localDateTimeToTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        long seconds = dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        int nanos = dateTime.getNano();
        return Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }
}
