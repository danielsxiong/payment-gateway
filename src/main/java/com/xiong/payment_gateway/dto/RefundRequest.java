package com.xiong.payment_gateway.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String reason;
}
