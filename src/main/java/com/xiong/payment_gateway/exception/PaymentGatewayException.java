package com.xiong.payment_gateway.exception;

import org.springframework.http.HttpStatus;

public class PaymentGatewayException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public PaymentGatewayException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public PaymentGatewayException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, "PAYMENT_GATEWAY_ERROR");
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
