# gRPC Testing Guide

## Using grpcurl

`grpcurl` is a command-line tool for testing gRPC services. It's the gRPC equivalent of `curl`.

### Installation

**macOS:**
```bash
brew install grpcurl
```

**Linux (Ubuntu/Debian):**
```bash
apt-get install grpcurl
```

**Windows (Chocolatey):**
```bash
choco install grpcurl
```

**Or download from GitHub:**
https://github.com/fullstorydev/grpcurl/releases

### Basic Commands

#### List All Services

```bash
grpcurl -plaintext localhost:9090 list
```

Output:
```
com.xiong.payment_gateway.grpc.PaymentService
com.xiong.payment_gateway.grpc.RefundService
```

#### Describe a Service

```bash
grpcurl -plaintext localhost:9090 describe com.xiong.payment_gateway.grpc.PaymentService
```

#### Describe a Message Type

```bash
grpcurl -plaintext localhost:9090 describe com.xiong.payment_gateway.grpc.PaymentRequest
```

## Test Cases

### 1. Create Payment (Success)

```bash
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_001",
    "amount": "150.50",
    "currency": "USD",
    "idempotency_key": "payment-001-'$(date +%s)'",
    "customer_id": "customer_001",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment",
    "metadata": {
      "order_id": "order_12345",
      "description": "Premium subscription"
    }
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

**Expected Response:**
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "amount": "150.50",
  "currency": "USD",
  "createdAt": {
    "seconds": "1674160000",
    "nanos": 123456789
  },
  "message": "Payment processed successfully"
}
```

### 2. Get Payment Details

Using the transaction ID from the previous response:

```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/GetPayment
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "merchantId": "merchant_001",
  "amount": "150.50",
  "currency": "USD",
  "status": "SUCCESS",
  "idempotencyKey": "payment-001-1674160000",
  "customerId": "customer_001",
  "paymentMethod": "CREDIT_CARD",
  "metadata": {
    "order_id": "order_12345",
    "description": "Premium subscription"
  },
  "createdAt": {...},
  "updatedAt": {...}
}
```

### 3. Create Refund (Full)

```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "150.50",
    "reason": "Customer requested refund"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.RefundService/CreateRefund
```

**Expected Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": "150.50",
  "reason": "Customer requested refund",
  "status": "COMPLETED",
  "createdAt": {
    "seconds": "1674160100",
    "nanos": 987654321
  }
}
```

### 4. Create Partial Refund

```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "50.00",
    "reason": "Partial refund - wrong item"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.RefundService/CreateRefund
```

### 5. Test Idempotency

Send the same payment request twice with the same idempotency key:

```bash
# First call - creates the payment
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_002",
    "amount": "100.00",
    "currency": "EUR",
    "idempotency_key": "idempotent-payment-123",
    "payment_method": "DEBIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

Save the transaction ID. Then call with the same key again:

```bash
# Second call - returns cached result with same transaction ID
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_002",
    "amount": "100.00",
    "currency": "EUR",
    "idempotency_key": "idempotent-payment-123",
    "payment_method": "DEBIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

Both calls should return the same transaction ID, confirming idempotency.

### 6. Test Error Cases

#### Missing Required Field

```bash
grpcurl -plaintext \
  -d '{
    "amount": "100.00",
    "currency": "USD"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

**Expected Error:**
```
Code: INVALID_ARGUMENT
Message: Validation failed: Merchant ID is required
```

#### Invalid Currency

```bash
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_003",
    "amount": "100.00",
    "currency": "INVALID",
    "idempotency_key": "test-invalid-currency",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

**Expected Error:**
```
Code: INVALID_ARGUMENT
Message: Currency must be 3 characters
```

#### Invalid Amount

```bash
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_003",
    "amount": "-50.00",
    "currency": "USD",
    "idempotency_key": "test-invalid-amount",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

**Expected Error:**
```
Code: INVALID_ARGUMENT
Message: Amount must be greater than 0
```

#### Transaction Not Found

```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "invalid-uuid-12345"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/GetPayment
```

**Expected Error:**
```
Code: NOT_FOUND
Message: Transaction not found
```

## Using Evans CLI (Alternative)

`evans` is another gRPC client that provides an interactive REPL.

### Installation

```bash
brew install evans
```

### Interactive Mode

```bash
evans -r -p 9090 -H localhost
```

Then in the REPL:

```
> package com.xiong.payment_gateway.grpc
> service PaymentService
> call CreatePayment
merchant_id (TYPE_STRING) => merchant_001
amount (TYPE_STRING) => 100.00
currency (TYPE_STRING) => USD
idempotency_key (TYPE_STRING) => test-evans-1
customer_id (TYPE_STRING) => customer_001
payment_method (TYPE_STRING) => CREDIT_CARD
webhook_url (TYPE_STRING) => https://webhook.example.com/payment
metadata (TYPE_MESSAGE <PaymentRequest.MetadataEntry>) => {"key":"order_id","value":"12345"}
{
  "transactionId": "...",
  "status": "SUCCESS",
  ...
}
```

## Using Postman (gRPC Support)

Postman 9.0+ supports gRPC:

1. Create new gRPC request
2. Enter server URL: `grpc://localhost:9090`
3. Select service and method
4. Enter request body as JSON
5. Click Send

## Batch Testing Script

Create a file `test_grpc.sh`:

```bash
#!/bin/bash

HOST="localhost"
PORT="9090"

echo "=== Testing Payment Service ==="

# Test 1: Create Payment
echo -e "\n1. Creating payment..."
PAYMENT_RESPONSE=$(grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_batch",
    "amount": "99.99",
    "currency": "USD",
    "idempotency_key": "batch-test-'$(date +%s%N)'",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  $HOST:$PORT \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment)

echo "$PAYMENT_RESPONSE"

# Extract transaction ID
TRANSACTION_ID=$(echo "$PAYMENT_RESPONSE" | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
echo -e "\nTransaction ID: $TRANSACTION_ID"

# Test 2: Get Payment
echo -e "\n2. Getting payment details..."
grpcurl -plaintext \
  -d "{\"transaction_id\":\"$TRANSACTION_ID\"}" \
  $HOST:$PORT \
  com.xiong.payment_gateway.grpc.PaymentService/GetPayment

# Test 3: Create Refund
echo -e "\n3. Creating refund..."
grpcurl -plaintext \
  -d "{\"transaction_id\":\"$TRANSACTION_ID\",\"amount\":\"50.00\",\"reason\":\"Partial refund\"}" \
  $HOST:$PORT \
  com.xiong.payment_gateway.grpc.RefundService/CreateRefund

echo -e "\n=== Tests Complete ==="
```

Run the script:
```bash
chmod +x test_grpc.sh
./test_grpc.sh
```

## Performance Testing

### Load Testing with ghz

`ghz` is a tool for load testing gRPC services.

**Installation:**
```bash
brew install ghz
```

**Load test create payment:**
```bash
ghz --insecure \
  --proto ./src/main/proto/payment.proto \
  --call com.xiong.payment_gateway.grpc.PaymentService/CreatePayment \
  -m '{
    "merchant_id": "load_test",
    "amount": "100.00",
    "currency": "USD",
    "idempotency_key": "load-test-{{conn}}-{{index}}",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment"
  }' \
  -c 100 \
  -n 10000 \
  localhost:9090
```

This will:
- Send 10,000 requests
- Using 100 concurrent connections
- Each with a unique idempotency key

## Debugging

### Enable gRPC Logging

Add to `application.yaml`:

```yaml
logging:
  level:
    io.grpc: DEBUG
    net.devh: DEBUG
```

### Monitor Network Traffic

```bash
# Monitor with tcpdump (macOS/Linux)
sudo tcpdump -i lo tcp port 9090 -vvv

# Or use Wireshark for GUI analysis
```

### Check Server Status

```bash
# Health check endpoint
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check

# List all services
grpcurl -plaintext localhost:9090 list
```

## Test Coverage

| Feature | Test Case | Expected Result |
|---------|-----------|-----------------|
| Create Payment | Valid request | SUCCESS status |
| Create Payment | Duplicate idempotency key | Same transaction ID |
| Create Payment | Missing merchant_id | INVALID_ARGUMENT error |
| Get Payment | Valid transaction ID | Transaction details |
| Get Payment | Invalid transaction ID | NOT_FOUND error |
| Create Refund | Valid request | COMPLETED status |
| Create Refund | Refund > transaction amount | Error |
| Create Refund | Non-existent transaction | NOT_FOUND error |

## CI/CD Integration

### GitHub Actions Example

```yaml
name: gRPC Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: ./gradlew build
      - run: ./gradlew bootRun &
      - run: sleep 5
      - run: grpcurl -plaintext localhost:9090 list
```

## Troubleshooting Test Issues

### Connection Refused
```
Error: dial tcp localhost:9090: connect: connection refused
```
**Solution:** Ensure the server is running: `./gradlew bootRun`

### Proto Compilation Error
```
Error: Unable to find method in service
```
**Solution:** Rebuild protos: `./gradlew clean build`

### Invalid JSON in Request
```
Error: invalid json message
```
**Solution:** Ensure JSON is properly formatted and quoted

### Timeout Issues
```
Error: context deadline exceeded
```
**Solution:** Add timeout flag: `grpcurl -plaintext -max-time 30 ...`
