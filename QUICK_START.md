# gRPC Quick Start Checklist

## ✅ Implementation Complete

All gRPC endpoints have been successfully implemented. Use this checklist to get started.

## Pre-requisites

- [ ] Java 17+ installed
- [ ] Gradle 8.0+ installed (or use gradlew)
- [ ] PostgreSQL running on localhost:5432
- [ ] Redis running on localhost:6379
- [ ] Database `payment_gateway` created

## Step 1: Build the Project

```bash
cd d:\Projects\payment-gateway
./gradlew clean build
```

**What happens:**
- Proto files compile to Java
- gRPC stubs and services generate
- Full project builds successfully

**Expected output ends with:**
```
BUILD SUCCESSFUL in Xs
```

## Step 2: Start the Server

```bash
./gradlew bootRun
```

**Expected output contains:**
```
Started PaymentGatewayApplication in X.XXs
```

**Services available:**
- REST API: http://localhost:8080
- gRPC API: localhost:9090

## Step 3: Test gRPC Services

### Option A: Using grpcurl (Recommended)

**Install grpcurl:**
```bash
# macOS
brew install grpcurl

# Windows
choco install grpcurl

# Or download: https://github.com/fullstorydev/grpcurl/releases
```

**Test the connection:**
```bash
grpcurl -plaintext localhost:9090 list
```

**Expected output:**
```
com.xiong.payment_gateway.grpc.PaymentService
com.xiong.payment_gateway.grpc.RefundService
```

### Option B: Using Postman

1. Open Postman
2. Create new gRPC request
3. Enter: `grpc://localhost:9090`
4. Select service and method
5. Enter JSON request body

### Option C: Using Java Client

Run the example client:
```bash
./gradlew runPaymentClient
```

Or compile and run:
```bash
cd src/main/java/com/xiong/payment_gateway/grpc/client
javac PaymentGrpcClientExample.java
java PaymentGrpcClientExample
```

## Example Requests

### Create a Payment

```bash
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_123",
    "amount": "99.99",
    "currency": "USD",
    "idempotency_key": "unique-key-'$(date +%s)'",
    "customer_id": "customer_456",
    "payment_method": "CREDIT_CARD",
    "webhook_url": "https://webhook.example.com/payment",
    "metadata": {
      "order_id": "order_789"
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
  "amount": "99.99",
  "currency": "USD",
  "message": "Payment processed successfully"
}
```

### Get Payment Details

```bash
grpcurl -plaintext \
  -d '{"transaction_id": "550e8400-e29b-41d4-a716-446655440000"}' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/GetPayment
```

### Create a Refund

```bash
grpcurl -plaintext \
  -d '{
    "transaction_id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "50.00",
    "reason": "Partial refund"
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.RefundService/CreateRefund
```

## Verify Both APIs Work

### Test REST API

```bash
# Create payment via REST
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "merchant_123",
    "amount": 99.99,
    "currency": "USD",
    "idempotencyKey": "rest-test-1",
    "paymentMethod": "CREDIT_CARD",
    "webhookUrl": "https://webhook.example.com/payment"
  }'
```

### Test gRPC API

```bash
# Create payment via gRPC (command shown above)
grpcurl -plaintext \
  -d '{
    "merchant_id": "merchant_123",
    ...
  }' \
  localhost:9090 \
  com.xiong.payment_gateway.grpc.PaymentService/CreatePayment
```

**Result:** Both should work simultaneously! ✅

## Documentation

| Document | Purpose | Location |
|----------|---------|----------|
| Quick Start | You are here | QUICK_START.md |
| Full Spec | Complete API reference | GRPC_IMPLEMENTATION.md |
| Testing Guide | How to test gRPC services | GRPC_TESTING.md |
| README | Overview and features | GRPC_README.md |
| Summary | Implementation details | IMPLEMENTATION_SUMMARY.md |

## Troubleshooting

### Build Fails
```bash
# Clean and rebuild
./gradlew clean build

# Or force proto recompile
./gradlew clean :compileProto build
```

### Connection Refused
```
Error: dial tcp localhost:9090: connect: connection refused
```
- Ensure server is running: `./gradlew bootRun`
- Check port 9090 is not in use: `lsof -i :9090` (macOS/Linux)

### Proto Compilation Error
```bash
# Rebuild all sources
./gradlew build --refresh-dependencies
```

### Database Connection Error
- Ensure PostgreSQL is running
- Check credentials in `src/main/resources/application.yaml`
- Verify database `payment_gateway` exists

### Redis Connection Error
- Ensure Redis is running on localhost:6379
- For idempotency to work, Redis must be available

## Key Configuration

All settings in `src/main/resources/application.yaml`:

```yaml
# gRPC Server Configuration
grpc:
  server:
    port: 9090                    # Change if needed

# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment_gateway
    username: postgres
    password: postgres

# Redis (for idempotency)
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## Architecture

```
Client
  ├─ REST Requests → Port 8080 → PaymentController/RefundController
  └─ gRPC Requests → Port 9090 → PaymentGrpcService/RefundGrpcService
                                      ↓
                          PaymentService (shared logic)
                          RefundService (shared logic)
                                      ↓
                          Database (PostgreSQL)
                          Cache (Redis)
```

## Next Steps

1. ✅ **Build & Run** - You just did this
2. **Test** - Use examples above to test endpoints
3. **Integrate** - Use gRPC clients in your applications
4. **Monitor** - Check logs and metrics
5. **Scale** - Configure for production use

## Production Checklist

Before deploying to production:

- [ ] Enable TLS/SSL for gRPC
- [ ] Set up authentication/authorization
- [ ] Configure rate limiting
- [ ] Enable request validation
- [ ] Set up monitoring and alerting
- [ ] Document API contracts
- [ ] Plan migration strategy (REST → gRPC)
- [ ] Set up load balancing
- [ ] Configure connection pooling
- [ ] Implement circuit breakers

## Performance Tips

- Use gRPC for backend-to-backend communication
- Keep REST for web/mobile clients
- Monitor gRPC metrics: latency, throughput, errors
- Consider streaming for high-volume operations
- Implement connection pooling in clients

## Support Resources

- **gRPC Documentation**: https://grpc.io/docs/languages/java/
- **Protocol Buffers**: https://developers.google.com/protocol-buffers
- **grpcurl GitHub**: https://github.com/fullstorydev/grpcurl
- **Spring gRPC**: https://grpc.io/blog/grpc-spring/

## Common Tasks

### View Available Services
```bash
grpcurl -plaintext localhost:9090 list
```

### Describe a Service
```bash
grpcurl -plaintext localhost:9090 describe com.xiong.payment_gateway.grpc.PaymentService
```

### Describe a Message
```bash
grpcurl -plaintext localhost:9090 describe com.xiong.payment_gateway.grpc.PaymentRequest
```

### Test with Batch Script
See [GRPC_TESTING.md](GRPC_TESTING.md) for batch test script

### Load Test
See [GRPC_TESTING.md](GRPC_TESTING.md) for ghz load testing

## Status

✅ **gRPC Implementation: COMPLETE**

All endpoints implemented and documented. Ready for testing and integration.

---

**Need Help?**
1. Check [GRPC_IMPLEMENTATION.md](GRPC_IMPLEMENTATION.md) for detailed API docs
2. Review [GRPC_TESTING.md](GRPC_TESTING.md) for testing procedures
3. See example clients in `src/main/java/.../grpc/client/`
