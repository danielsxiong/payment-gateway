package com.xiong.payment_gateway.grpc;

import com.google.protobuf.Timestamp;
import com.xiong.payment_gateway.dto.RefundRequest;
import com.xiong.payment_gateway.models.Refund;
import com.xiong.payment_gateway.service.RefundService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@GrpcService
@Slf4j
public class RefundGrpcService extends RefundServiceGrpc.RefundServiceImplBase {
    
    private final RefundService refundService;
    
    public RefundGrpcService(RefundService refundService) {
        this.refundService = refundService;
    }
    
    @Override
    public void createRefund(
            com.xiong.payment_gateway.grpc.RefundRequest request,
            StreamObserver<com.xiong.payment_gateway.grpc.Refund> responseObserver) {
        try {
            log.info("gRPC: Processing refund for transaction: {}", request.getTransactionId());
            
            // Convert proto request to DTO
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setTransactionId(request.getTransactionId());
            refundRequest.setAmount(new BigDecimal(request.getAmount()));
            refundRequest.setReason(request.getReason());
            
            // Process refund using existing service
            Refund refund = refundService.processRefund(refundRequest);
            
            // Convert model to proto response
            com.xiong.payment_gateway.grpc.Refund protoRefund = 
                    convertToProtoRefund(refund);
            
            responseObserver.onNext(protoRefund);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error processing refund via gRPC", e);
            responseObserver.onError(e);
        }
    }
    
    private com.xiong.payment_gateway.grpc.Refund convertToProtoRefund(Refund refund) {
        return com.xiong.payment_gateway.grpc.Refund.newBuilder()
                .setId(refund.getId())
                .setTransactionId(refund.getTransactionId())
                .setAmount(refund.getAmount().toString())
                .setReason(refund.getReason() != null ? refund.getReason() : "")
                .setStatus(refund.getStatus() != null ? refund.getStatus().toString() : "")
                .setCreatedAt(localDateTimeToTimestamp(refund.getCreatedAt()))
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
