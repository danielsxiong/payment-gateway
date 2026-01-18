# Copilot Instructions for Payment Gateway

## Project Overview
Payment Gateway is a Spring Boot 4.0 REST API application (Java 17) for processing and managing payment transactions. It uses PostgreSQL for persistence, Redis for idempotency tracking, and supports refunds and webhook notifications.

## Architecture Overview

### High-Level Data Flow
1. **Payment Request** → PaymentController → PaymentService
2. **Idempotency Check** → Redis (24-hour TTL)
3. **Transaction Processing** → PaymentRepository (PostgreSQL)
4. **Webhook Notification** → WebhookService (async via @Async)
5. **Refund Support** → RefundService (tracks partial/full refunds)

### Core Models & Entities

| Model | Table | Purpose | Key Fields |
|-------|-------|---------|-----------|
| [PaymentTransaction](src/main/java/com/xiong/payment_gateway/models/PaymentTransaction.java) | `payment_transactions` | Main transaction record | `id` (UUID), `merchantId`, `amount`, `idempotencyKey` (unique), `status` |
| [Refund](src/main/java/com/xiong/payment_gateway/models/Refund.java) | `refunds` | Refund records (partial/full) | `id` (UUID), `transactionId`, `amount`, `status` |
| [WebhookEvent](src/main/java/com/xiong/payment_gateway/models/WebhookEvent.java) | `webhook_events` | Webhook delivery tracking | `id` (UUID), `transactionId`, `eventType`, `payload`, `status`, `attempts` |

### Service Layer Architecture

**PaymentService** ([src/main/java/com/xiong/payment_gateway/service/PaymentService.java](src/main/java/com/xiong/payment_gateway/service/PaymentService.java))
- `processPayment(PaymentRequest)`: Main payment flow with idempotency check
- Uses IdempotencyService for duplicate detection
- Calls WebhookService for async notification
- Mock payment provider integration (90% success rate for demo)

**RefundService** ([src/main/java/com/xiong/payment_gateway/service/RefundService.java](src/main/java/com/xiong/payment_gateway/service/RefundService.java))
- `processRefund(RefundRequest)`: Full or partial refunds
- Validates refund amount against transaction balance
- Updates transaction status to `REFUNDED` or `PARTIAL_REFUND`
- Sends refund webhooks

**IdempotencyService** ([src/main/java/com/xiong/payment_gateway/service/IdempotencyService.java](src/main/java/com/xiong/payment_gateway/service/IdempotencyService.java))
- Uses Redis with 24-hour TTL for idempotency keys
- Key format: `idempotency:<idempotencyKey>` → `<transactionId>`
- Returns existing transaction ID if already processed

**WebhookService** ([src/main/java/com/xiong/payment_gateway/service/WebhookService.java](src/main/java/com/xiong/payment_gateway/service/WebhookService.java))
- `@Async` methods for non-blocking webhook delivery
- Events: `payment.completed`, `refund.completed`
- Stores WebhookEvent records for retry tracking
- Mock delivery with failure handling

### API Endpoints

| Endpoint | Method | Request | Response |
|----------|--------|---------|----------|
| `/api/v1/payments` | POST | PaymentRequest | PaymentResponse |
| `/api/v1/payments/{transactionId}` | GET | - | PaymentTransaction |
| `/api/v1/refunds` | POST | RefundRequest | Refund |

### Request/Response Models

**PaymentRequest** ([src/main/java/com/xiong/payment_gateway/dto/PaymentRequest.java](src/main/java/com/xiong/payment_gateway/dto/PaymentRequest.java))
- Required: `merchantId`, `amount`, `currency` (3 chars), `idempotencyKey`, `paymentMethod`, `webhookUrl` (HTTPS)
- Optional: `customerId`, `metadata`

**RefundRequest** ([src/main/java/com/xiong/payment_gateway/dto/RefundRequest.java](src/main/java/com/xiong/payment_gateway/dto/RefundRequest.java))
- Required: `transactionId`, `amount`
- Optional: `reason`

## Build & Development

### Build System
- **Gradle** with Spring Boot plugin (v4.0.1)
- **Java version**: 17 via toolchain
- **Key dependencies**: spring-boot-starter-data-jpa, spring-boot-starter-data-redis, spring-boot-starter-webmvc, spring-boot-starter-validation, postgresql driver

### Common Tasks
```bash
# Build
./gradlew build          # or gradlew.bat on Windows

# Run
./gradlew bootRun

# Test
./gradlew test
```

### Configuration
- **application.yaml**: PostgreSQL (`localhost:5432`), Redis (`localhost:6379`), port `8080`
- **Webhook retry**: 3 max attempts, 60-second delay
- **SQL logging**: Enabled by default (`spring.jpa.show-sql: true`) — disable for production

## Development Patterns

### Entity Design
1. **UUID Primary Keys**: All entities use `@GeneratedValue(strategy = GenerationType.UUID)`
2. **Timestamp Lifecycle**: Use `@PrePersist`/`@PreUpdate` hooks, not database defaults
3. **Enum Status**: Mapped with `@Enumerated(EnumType.STRING)` for readability
4. **Unique Constraints**: `idempotencyKey` enforced at DB level via `@Column(unique = true)`

### Idempotency Pattern
- Client provides `idempotencyKey` (UUID recommended)
- PaymentService checks Redis before processing
- If exists → return cached transaction ID
- If new → process and store in Redis + database
- Prevents duplicate charges on network retries

### Refund Logic
- Only `SUCCESS` or `PARTIAL_REFUND` transactions can be refunded
- Refund amount validated against remaining balance
- Auto-updates parent transaction status
- Multiple partial refunds tracked via RefundRepository

### Status Enums
- **TransactionStatus**: `PENDING` → `PROCESSING` → `SUCCESS`/`FAILED`, with `REFUNDED`/`PARTIAL_REFUND`
- **RefundStatus**: `PENDING` → `COMPLETED`/`FAILED`
- **WebhookStatus**: `PENDING` → `DELIVERED`/`FAILED`

### Async Processing
- `@EnableAsync` in [AppConfig](src/main/java/com/xiong/payment_gateway/config/AppConfig.java) activates async processing
- `@Async` methods (WebhookService) run in background threads
- Webhook payloads serialized via ObjectMapper bean

### Repository Queries
```java
// Custom queries used:
PaymentRepository.findByIdempotencyKey(String) — for idempotency lookup
RefundRepository.findByTransactionId(String) — for refund history
WebhookRepository.findByStatus(WebhookStatus) — for retry processing
```

## Key Files Reference
| File | Purpose |
|------|---------|
| [build.gradle](build.gradle) | Dependencies, Java 17 toolchain |
| [src/main/resources/application.yaml](src/main/resources/application.yaml) | DB/Redis config, webhook retry settings |
| [src/main/java/com/xiong/payment_gateway/config/AppConfig.java](src/main/java/com/xiong/payment_gateway/config/AppConfig.java) | RestTemplate, ObjectMapper beans, @EnableAsync |
| [src/main/java/com/xiong/payment_gateway/controller/](src/main/java/com/xiong/payment_gateway/controller/) | PaymentController, RefundController |
| [src/main/java/com/xiong/payment_gateway/service/](src/main/java/com/xiong/payment_gateway/service/) | PaymentService, RefundService, IdempotencyService, WebhookService |
| [src/main/java/com/xiong/payment_gateway/models/](src/main/java/com/xiong/payment_gateway/models/) | Entities and enums |

## Important Gotchas
- **Package name**: Uses `com.xiong.payment_gateway` (underscore), not `com.xiong.payment-gateway` (hyphen)
- **Idempotency TTL**: 24 hours in Redis — requests with old idempotency keys after 24 hours will be processed again
- **Webhook async**: Fire-and-forget; failures logged but don't block payment response
- **Mock payment provider**: 90% success rate in mock — replace with real provider integration
- **SQL logging**: On by default — disable `spring.jpa.show-sql` in production
- **Refund validation**: Must validate against total already refunded before processing
