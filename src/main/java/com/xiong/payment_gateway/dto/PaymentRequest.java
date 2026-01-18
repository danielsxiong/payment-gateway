package com.xiong.payment_gateway.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String customerId;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Webhook URL is required")
    @Pattern(regexp = "^https?://.*", message = "Webhook URL must be valid")
    private String webhookUrl;

    private Object metadata;
}
