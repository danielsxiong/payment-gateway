package com.xiong.payment_gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiong.payment_gateway.dto.RefundRequest;
import com.xiong.payment_gateway.models.PaymentTransaction;
import com.xiong.payment_gateway.models.Refund;
import com.xiong.payment_gateway.models.RefundStatus;
import com.xiong.payment_gateway.models.TransactionStatus;
import com.xiong.payment_gateway.repository.PaymentRepository;
import com.xiong.payment_gateway.repository.RefundRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookService webhookService;

    public RefundService(
        RefundRepository refundRepository,
        PaymentRepository paymentRepository,
        WebhookService webhookService
    ) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.webhookService = webhookService;
    }

    @Transactional
    public Refund processRefund(RefundRequest request) {
        PaymentTransaction transaction = paymentRepository
            .findById(request.getTransactionId())
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.SUCCESS &&
            transaction.getStatus() != TransactionStatus.PARTIAL_REFUND) {
            throw new RuntimeException("Transaction cannot be refunded");
        }

        // Calculate total refunded amount
        List<Refund> existingRefunds = refundRepository
            .findByTransactionId(request.getTransactionId());
        BigDecimal totalRefunded = existingRefunds.stream()
            .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
            .map(Refund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = transaction.getAmount().subtract(totalRefunded);
        
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new RuntimeException("Refund amount exceeds remaining amount");
        }

        // Create refund
        Refund refund = new Refund();
        refund.setTransactionId(request.getTransactionId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus(RefundStatus.PENDING);
        refund = refundRepository.save(refund);

        // Process refund (mock)
        boolean success = processWithPaymentProvider(refund);
        
        if (success) {
            refund.setStatus(RefundStatus.COMPLETED);
            
            // Update transaction status
            BigDecimal newTotal = totalRefunded.add(request.getAmount());
            if (newTotal.compareTo(transaction.getAmount()) == 0) {
                transaction.setStatus(TransactionStatus.REFUNDED);
            } else {
                transaction.setStatus(TransactionStatus.PARTIAL_REFUND);
            }
            paymentRepository.save(transaction);
            
            log.info("Refund completed: {}", refund.getId());
        } else {
            refund.setStatus(RefundStatus.FAILED);
            log.error("Refund failed: {}", refund.getId());
        }
        
        refund = refundRepository.save(refund);
        
        // Send webhook
        webhookService.sendRefundWebhook(refund, transaction);
        
        return refund;
    }

    private boolean processWithPaymentProvider(Refund refund) {
        // Mock refund processing
        return Math.random() < 0.95;
    }
}