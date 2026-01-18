package com.xiong.payment_gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime createdAt;
    private String message;
}