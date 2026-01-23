# gRPC Architecture & Design

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Client Applications                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│     Web Clients           Mobile Apps          Backend Services            │
│     (Browser)             (Mobile SDKs)        (Microservices)             │
│           │                    │                      │                     │
│           │                    │                      │                     │
│           └────────────────────┼──────────────────────┘                     │
│                                │                                            │
│                                ▼                                            │
│                    ┌─────────────────────────┐                             │
│                    │   Dual Protocol Gateway │                             │
│                    ├─────────────────────────┤                             │
│                    │  REST API  │  gRPC API  │                             │
│                    │  Port 8080 │  Port 9090 │                             │
│                    └──────┬──────┬──────┬─────┘                             │
└─────────────────────────────┼────────────┼──────────────────────────────────┘
                              │            │
                              │            │
         ┌────────────────────┼────────────┴──────────────────────┐
         │                    │                                    │
         ▼                    ▼                                    ▼
    ┌────────────┐      ┌──────────────┐                ┌────────────────┐
    │ REST Layer │      │ gRPC Layer   │                │  Config Layer  │
    ├────────────┤      ├──────────────┤                ├────────────────┤
    │Controllers │      │gRPC Services │                │  application.  │
    │  (Spring)  │      │ (Proto-based)│                │     yaml       │
    └─────┬──────┘      └──────┬───────┘                └────────────────┘
          │                    │
          │    ┌───────────────┘
          │    │
          ▼    ▼
    ┌──────────────────────────────────────────────────┐
    │           Shared Service Layer                    │
    ├──────────────────────────────────────────────────┤
    │  PaymentService  │  RefundService  │ Other Svcs  │
    │  (Business Logic)│  (Business Logic)│             │
    └────────────┬─────────────────┬─────────────────────┘
                 │                 │
                 │   ┌─────────────┘
                 │   │
         ┌───────▼───▼──────────────────────────────┐
         │    Data Access Layer                    │
         ├──────────────────────────────────────────┤
         │  PaymentRepository  │  RefundRepository  │
         │  (JPA Entities)     │  (JPA Entities)    │
         └────────┬──────────────────┬───────────────┘
                  │                  │
         ┌────────▼──────────┬───────▼────────┐
         │                   │                │
         ▼                   ▼                ▼
    ┌─────────────┐   ┌─────────────┐  ┌─────────────┐
    │ PostgreSQL  │   │   Redis     │  │  External   │
    │ Database    │   │   Cache     │  │  Webhooks   │
    │             │   │ Idempotency │  │             │
    └─────────────┘   └─────────────┘  └─────────────┘
```

## Request Flow Comparison

### REST Flow (HTTP/1.1)

```
Client
  │
  ├─ POST /api/v1/payments (JSON)
  │  ┌────────────────────────────┐
  │  │ HTTP/1.1 Connection        │
  │  │ - New connection per call   │
  │  │ - Serialized as JSON        │
  │  └────────────────────────────┘
  │
  ▼ PaymentController
    │
    ├─ @PostMapping("/payments")
    │
    ▼ PaymentService
      │
      ├─ checkIdempotency()
      │
      ├─ processPayment()
      │
      ├─ saveTransaction()
      │
      ├─ sendWebhook() (@Async)
      │
      ▼ Response (JSON)
        │
        └─ HTTP 201 Created
           ┌────────────────────────────┐
           │ {                          │
           │   "transactionId": "...",  │
           │   "status": "SUCCESS",     │
           │   ...                      │
           │ }                          │
           └────────────────────────────┘
```

### gRPC Flow (HTTP/2)

```
Client
  │
  ├─ gRPC Call: CreatePayment (Proto)
  │  ┌────────────────────────────┐
  │  │ HTTP/2 Connection          │
  │  │ - Reusable connection       │
  │  │ - Multiplexed streams       │
  │  │ - Binary Protocol Buffers   │
  │  └────────────────────────────┘
  │
  ▼ PaymentGrpcService
    │
    ├─ Convert Proto → DTO
    │
    ├─ Call PaymentService (shared)
    │
    ├─ Convert Response → Proto
    │
    ▼ Response (Binary)
      │
      └─ gRPC OK
         ┌────────────────────────────┐
         │ PaymentResponse {          │
         │   transactionId: "...",    │
         │   status: "SUCCESS",       │
         │   ...                      │
         │ }                          │
         └────────────────────────────┘
```

## Proto Message Structure

### PaymentRequest Message

```protobuf
message PaymentRequest {
  string merchant_id         // Required: Merchant identifier
  string amount              // Required: Amount as string (BigDecimal)
  string currency            // Required: 3-char currency code
  string idempotency_key     // Required: Unique key for idempotency
  string customer_id         // Optional: Customer identifier
  string payment_method      // Required: Payment method type
  string webhook_url         // Required: HTTPS webhook URL
  map<string,string> metadata // Optional: Additional data
}
```

### PaymentResponse Message

```protobuf
message PaymentResponse {
  string transaction_id           // Created transaction ID (UUID)
  string status                   // Transaction status (enum string)
  string amount                   // Amount as string
  string currency                 // Currency code
  google.protobuf.Timestamp created_at  // Creation timestamp
  string message                  // Success/error message
}
```

### PaymentTransaction Message

```protobuf
message PaymentTransaction {
  string id                  // Transaction ID
  string merchant_id         // Merchant ID
  string amount              // Amount
  string currency            // Currency code
  string status              // Current status
  string idempotency_key     // Used idempotency key
  string customer_id         // Customer ID
  string payment_method      // Payment method used
  map<string,string> metadata // Additional metadata
  google.protobuf.Timestamp created_at  // Created at
  google.protobuf.Timestamp updated_at  // Updated at
}
```

## Service Inheritance Model

```
┌─────────────────────────────────────────┐
│      PaymentServiceImplBase             │
│      (gRPC Generated Base Class)        │
└────────────┬────────────────────────────┘
             │ extends
             │
┌────────────▼────────────────────────────┐
│     PaymentGrpcService                  │
│     (Our Implementation)                │
├─────────────────────────────────────────┤
│ Methods:                                │
│  - createPayment()                      │
│  - getPayment()                         │
│                                         │
│ Dependencies:                           │
│  - PaymentService (injected)            │
│  - Helper methods for conversion        │
└────────────┬────────────────────────────┘
             │
             ▼ calls
        PaymentService
        (Shared Business Logic)
```

## Data Type Mapping

### Java ↔ Proto Conversions

```
Java Type           Proto Type              Notes
─────────────────────────────────────────────────────────
BigDecimal          string                  Stored as "99.99"
String              string                  Direct mapping
LocalDateTime       google.protobuf.        Timezone-aware
                    Timestamp
Map<K,V>            map<K,V>               Direct mapping
Enum                string                  Stored as name
UUID                string                  Stored as UUID string
```

### Example Type Conversion

```java
// Java PaymentRequest
PaymentRequest javaReq = new PaymentRequest();
javaReq.setMerchantId("merchant_123");
javaReq.setAmount(new BigDecimal("99.99"));
javaReq.setCurrency("USD");

        ↓↓↓ (Our conversion code) ↓↓↓

// Proto PaymentRequest
PaymentRequest protoReq = PaymentRequest.newBuilder()
    .setMerchantId("merchant_123")
    .setAmount("99.99")
    .setCurrency("USD")
    .build();
```

## Error Handling Hierarchy

```
Exception
  │
  ├─ io.grpc.StatusRuntimeException
  │  │
  │  ├─ Status: INVALID_ARGUMENT
  │  │   └─ Validation failed: Merchant ID is required
  │  │
  │  ├─ Status: NOT_FOUND
  │  │   └─ Transaction not found
  │  │
  │  ├─ Status: ALREADY_EXISTS
  │  │   └─ Idempotency key already processed
  │  │
  │  └─ Status: INTERNAL
  │      └─ Database connection error
  │
  └─ com.xiong.payment_gateway.exception.*
     ├─ PaymentGatewayException
     ├─ ResourceNotFoundException
     └─ IdempotentDuplicateException
```

## Deployment Architecture

### Single Server (Development)

```
┌─────────────────────────────────────────────┐
│          Spring Boot Application            │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  REST Controller                     │  │
│  │  (Port 8080)                        │  │
│  └──────────────────┬───────────────────┘  │
│                     │                      │
│  ┌──────────────────▼───────────────────┐  │
│  │  gRPC Service                        │  │
│  │  (Port 9090)                        │  │
│  └──────────────────┬───────────────────┘  │
│                     │                      │
│  ┌──────────────────▼───────────────────┐  │
│  │  Shared Business Logic               │  │
│  └──────────────────┬───────────────────┘  │
│                     │                      │
└─────────────────────┼──────────────────────┘
                      │
         ┌────────────┼────────────┐
         │            │            │
         ▼            ▼            ▼
    PostgreSQL    Redis        Webhooks
    (Database)    (Cache)      (Async)
```

### Scaled Deployment (Production)

```
┌──────────────────────────────────────────────────────────────┐
│                    Load Balancer                             │
│              (Handles REST & gRPC)                           │
├──────────────┬──────────────────────────┬────────────────────┤
│ REST Route   │ REST Route               │ gRPC Route         │
│ (Port 8080)  │ (Port 8080)              │ (Port 9090)        │
└──────┬───────┴──────────┬───────────────┴────────┬───────────┘
       │                  │                        │
       ▼                  ▼                        ▼
   ┌─────────────┐    ┌─────────────┐        ┌─────────────┐
   │ Instance 1  │    │ Instance 2  │        │ Instance N  │
   │ Spring Boot │    │ Spring Boot │        │ Spring Boot │
   │ (R & gRPC)  │    │ (R & gRPC)  │        │ (R & gRPC)  │
   └──────┬──────┘    └──────┬──────┘        └──────┬──────┘
          │                  │                      │
          └──────────────────┼──────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
    PostgreSQL          Redis Cluster       Webhook Queue
    (Primary +          (High Avail.)      (Message Queue)
     Replicas)
```

## Performance Characteristics

### Serialization Performance

```
Protocol          Size    Speed     Use Case
─────────────────────────────────────────────────
JSON (REST)       ~2KB    100ms     Web/Browser
Protocol Buffers  ~600B   10ms      Backend/gRPC
(gRPC)            (70%    (10x      High-perf
                  smaller) faster)
```

### Throughput Comparison

```
                 REST API        gRPC API
────────────────────────────────────────────
1 connection     1 req/s         1 req/s
                 (blocking)

10 connections   10 req/s        50-100 req/s
                 (separate       (multiplexed
                  TCP)            HTTP/2)

100 connections  100 req/s       500-1000 req/s
                 (connections    (efficient
                  overhead)      streaming)
```

## Security Layers

```
┌──────────────────────────────────────┐
│       Client Request                 │
│  (REST/gRPC with credentials)        │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│    Transport Layer Security          │
│  (TLS/SSL encryption - optional)     │
│  (HTTP/2 protocol security)          │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  Authentication Interceptor          │
│  (Verify JWT/API Key - future)       │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  Authorization Check                 │
│  (Verify permissions - future)       │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  Input Validation                    │
│  (Proto validation + business rules) │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  Business Logic Processing           │
│  (PaymentService, RefundService)     │
└────────┬─────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│  Data Persistence                    │
│  (PostgreSQL + Redis)                │
└──────────────────────────────────────┘
```

## Implementation Timeline

```
Week 1
├─ Proto Definition ✅
├─ Service Implementation ✅
└─ Configuration ✅

Week 2
├─ Example Clients ✅
├─ Testing Guide ✅
└─ Documentation ✅

Week 3
├─ API Reference ✅
├─ Quick Start Guide ✅
└─ Deliverables Complete ✅
```

## Feature Matrix

```
Feature              REST API    gRPC API
─────────────────────────────────────────────
Idempotency          ✅          ✅
Webhook Async        ✅          ✅
Error Handling       ✅          ✅
Validation           ✅          ✅
Connection Reuse     Limited     ✅✅
Multiplexing         ✗           ✅
Binary Format        ✗           ✅
Type Safety          Partial     ✅✅
Performance          Good        Excellent
Browser Support      ✅          Needs gRPC-web
Mobile Support       ✅          ✅
Backend-to-Backend   Good        ✅✅ Excellent
```

---

For more details, see:
- GRPC_IMPLEMENTATION.md - Complete API specification
- GRPC_TESTING.md - Testing procedures
- QUICK_START.md - Getting started guide
