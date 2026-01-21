package com.xiong.payment_gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Response wrapper that includes the HTTP status to be returned.
 * Used to differentiate between 201 CREATED (new payment) and 200 OK (duplicate/idempotent).
 */
@Data
@Builder
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private HttpStatus statusCode;
    private String message;
    private boolean isDuplicate;
}
