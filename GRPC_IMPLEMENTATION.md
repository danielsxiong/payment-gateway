# gRPC Implementation Guide

## Overview

This document describes the gRPC endpoints implemented for the Payment Gateway service. All REST endpoints have been mirrored as gRPC services.

## gRPC Services

### 1. PaymentService

Located at: `src/main/proto/payment.proto`

#### Methods

##### `CreatePayment(PaymentRequest) → PaymentResponse`

Creates a new payment transaction.

**Request Fields:**
- `merchant_id` (string, required): The merchant identifier
- `amount` (string, required): Payment amount in decimal format (e.g., "99.99")
- `currency` (string, required): 3-character currency code (e.g., "USD", "EUR")
- `idempotency_key` (string, required): Unique key for idempotent requests
- `customer_id` (string, optional): Customer identifier
- `payment_method` (string, required): Payment method (e.g., "CREDIT_CARD", "DEBIT_CARD")
- `webhook_url` (string, required): HTTPS webhook URL for notifications
- `metadata` (map<string, string>, optional): Additional metadata as key-value pairs

**Response Fields:**
- `transaction_id` (string): UUID of the created transaction
- `status` (string): Transaction status (PENDING, PROCESSING, SUCCESS, FAILED)
- `amount` (string): Transaction amount
- `currency` (string): Currency code
- `created_at` (Timestamp): Transaction creation timestamp
- `message` (string): Response message

**Example Call:**
```bash
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_123",
    "amount": "99.99",
    "currency": "USD",
    "idempotency_key": "idempotent-key-1",
    "customer_id": "customer_456",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment",
    "metadata": {
      "order_id": "order_789",
      "description": "Product purchase"
    }
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

##### `GetPayment(GetPaymentRequest) → PaymentTransaction`

Retrieves a payment transaction by ID.

**Request Fields:**
- `transaction_id` (string, required): UUID of the transaction

**Response Fields:**
- `id` (string): Transaction ID
- `merchant_id` (string): Merchant identifier
- `amount` (string): Transaction amount
- `currency` (string): Currency code
- `status` (string): Transaction status
- `idempotency_key` (string): Idempotency key used
- `customer_id` (string): Customer identifier
- `payment_method` (string): Payment method used
- `metadata` (map<string, string>): Additional metadata
- `created_at` (Timestamp): Creation timestamp
- `updated_at` (Timestamp): Last update timestamp

**Example Call:**
```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/GetPayment
```

### 2. RefundService

Located at: `src/main/proto/refund.proto`

#### Methods

##### `CreateRefund(RefundRequest) → Refund`

Processes a refund for a payment transaction.

**Request Fields:**
- `transaction_id` (string, required): UUID of the payment transaction to refund
- `amount` (string, required): Refund amount in decimal format
- `reason` (string, optional): Reason for the refund (e.g., "Customer Request", "Product Return")

**Response Fields:**
- `id` (string): UUID of the refund record
- `transaction_id` (string): Original transaction ID
- `amount` (string): Refund amount
- `reason` (string): Refund reason
- `status` (string): Refund status (PENDING, COMPLETED, FAILED)
- `created_at` (Timestamp): Refund creation timestamp

**Example Call:**
```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "50.00",
    "reason": "Product Return"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.RefundService/CreateRefund
```

## Running the gRPC Server

### Start the Application

```bash
./gradlew bootRun
```

The gRPC server will start on port `9090` (configurable in `application.yaml` under `grpc.server.port`).

### Verify the Server is Running

```bash
grpcurl -plaintext localhost:9090 list
```

Expected output:
```
com.xiong.payment_gateway.grpc.PaymentService
com.xiong.payment_gateway.grpc.RefundService
```

## Testing with grpcurl

### Install grpcurl

**Linux/Mac:**
```bash
brew install grpcurl
```

**Windows:**
```bash
choco install grpcurl
```

Or download from: https://github.com/fullstorydev/grpcurl/releases

### List Available Services

```bash
grpcurl -plaintext localhost:9090 list
```

### Get Service Details

```bash
grpcurl -plaintext localhost:9090 describe com.xiong.payment_gateway.grpc.PaymentService
```

## Java Client Implementation

### Example gRPC Client Code

```java
import com.xiong.payment_gateway.grpc.*;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.protobuf.Timestamp;

public class PaymentGrpcClient {
    
    public static void main(String[] args) {
        // Create a channel
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        
        try {
            // Create a stub
            PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub = 
                    PaymentServiceGrpc.newBlockingStub(channel);
            
            // Create payment request
            PaymentRequest paymentRequest = PaymentRequest.newBuilder()
                    .setMerchantId("merchant_123")
                    .setAmount("99.99")
                    .setCurrency("USD")
                    .setIdempotencyKey("idempotent-key-1")
                    .setCustomerId("customer_456")
                    .setPaymentMethod("CREDIT_CARD")
                    .setWebhookUrl("https://webhook.example.com/payment")
                    .putMetadata("order_id", "order_789")
                    .putMetadata("description", "Product purchase")
                    .build();
            
            // Call the service
            PaymentResponse response = paymentStub.createPayment(paymentRequest);
            
            System.out.println("Transaction ID: " + response.getTransactionId());
            System.out.println("Status: " + response.getStatus());
            System.out.println("Amount: " + response.getAmount());
            System.out.println("Message: " + response.getMessage());
            
            // Get payment details
            GetPaymentRequest getRequest = GetPaymentRequest.newBuilder()
                    .setTransactionId(response.getTransactionId())
                    .build();
            
            PaymentTransaction transaction = paymentStub.getPayment(getRequest);
            
            System.out.println("\nTransaction Details:");
            System.out.println("Merchant ID: " + transaction.getMerchantId());
            System.out.println("Status: " + transaction.getStatus());
            System.out.println("Created At: " + transaction.getCreatedAt());
            
        } finally {
            channel.shutdownNow();
        }
    }
}
```

### Example Refund Client

```java
public class RefundGrpcClient {
    
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        
        try {
            RefundServiceGrpc.RefundServiceBlockingStub refundStub = 
                    RefundServiceGrpc.newBlockingStub(channel);
            
            // Create refund request
            RefundRequest refundRequest = RefundRequest.newBuilder()
                    .setTransactionId("550e8400-e29b-41d4-a716-446655440000")
                    .setAmount("50.00")
                    .setReason("Product Return")
                    .build();
            
            // Process refund
            Refund refund = refundStub.createRefund(refundRequest);
            
            System.out.println("Refund ID: " + refund.getId());
            System.out.println("Status: " + refund.getStatus());
            System.out.println("Amount: " + refund.getAmount());
            System.out.println("Created At: " + refund.getCreatedAt());
            
        } finally {
            channel.shutdownNow();
        }
    }
}
```

## Configuration Options

### gRPC Server Configuration (application.yaml)

```yaml
grpc:
  server:
    port: 9090                          # Port to listen on
    enable-keep-alive: true             # Enable keep-alive pings
    keep-alive-time: 30s                # Interval between keep-alive pings
    keep-alive-timeout: 10s             # Timeout for keep-alive response
    max-concurrent-streams: 100         # Max concurrent streams per connection
    in-process-executor-threads: 10     # Executor thread pool size
```

## Proto File Structure

### Messages

**Payment Service Messages:**
- `PaymentRequest`: Request to create a payment
- `PaymentResponse`: Response from payment creation
- `GetPaymentRequest`: Request to fetch payment details
- `PaymentTransaction`: Full payment transaction details

**Refund Service Messages:**
- `RefundRequest`: Request to create a refund
- `Refund`: Refund details

### Services

Both services are defined in their respective proto files:
- `PaymentService` in `payment.proto`
- `RefundService` in `refund.proto`

## Error Handling

Errors are returned as gRPC status codes:

- `INVALID_ARGUMENT`: Validation errors (e.g., invalid amount, missing fields)
- `NOT_FOUND`: Transaction/Refund not found
- `ALREADY_EXISTS`: Duplicate idempotency key (for payments)
- `INTERNAL`: Internal server errors
- `UNAVAILABLE`: Service unavailable (e.g., database connection issues)

Example error handling:

```java
try {
    PaymentResponse response = paymentStub.createPayment(paymentRequest);
} catch (io.grpc.StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();
    System.out.println("Error Code: " + status.getCode());
    System.out.println("Error Message: " + status.getDescription());
}
```

## REST vs gRPC Comparison

| Feature | REST | gRPC |
|---------|------|------|
| **Port** | 8080 | 9090 |
| **Protocol** | HTTP/1.1 | HTTP/2 |
| **Serialization** | JSON | Protocol Buffers |
| **Performance** | Standard | High-performance |
| **Streaming** | Limited | Full bidirectional streaming |
| **Browser Support** | Yes | No (requires gRPC-web) |

Both APIs are fully functional and can be used simultaneously. Choose gRPC for high-performance backend-to-backend communication and REST for web/mobile clients.

## Build and Compilation

The proto files are automatically compiled during the build process:

```bash
./gradlew build
```

Generated Java classes will be placed in:
- `build/generated/source/proto/main/java/com/xiong/payment_gateway/grpc/`

## Related Files

- Proto definitions: `src/main/proto/`
- gRPC Service implementations: `src/main/java/com/xiong/payment_gateway/grpc/`
- Configuration: `src/main/java/com/xiong/payment_gateway/config/GrpcConfig.java`
- Application config: `src/main/resources/application.yaml`
