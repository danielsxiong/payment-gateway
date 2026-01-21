# Error Handling & HTTP Status Codes

This document describes the comprehensive error handling implemented across all Payment Gateway API endpoints.

## Global Exception Handler

All exceptions are handled by the `GlobalExceptionHandler` (`@RestControllerAdvice`) which returns standardized error responses with proper HTTP status codes.

### Error Response Format

All error responses follow this structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "path": "/api/v1/payments",
  "timestamp": "2026-01-21T10:30:00",
  "details": ["field1: validation error", "field2: validation error"],
  "traceId": "uuid"
}
```

**Fields:**
- `status`: HTTP status code
- `error`: HTTP status reason phrase
- `message`: Descriptive error message
- `path`: API endpoint path
- `timestamp`: Error occurrence time (ISO 8601)
- `details`: Optional array of validation errors (only for validation errors)
- `traceId`: Unique trace ID for error tracking and logging

---

## API Endpoints & Error Handling

### 1. POST /api/v1/payments - Create Payment

**Success Response (First Request):**
- **Status Code**: `201 Created`
- **Response**: PaymentResponse object

**Success Response (Duplicate Idempotency Key):**
- **Status Code**: `409 Conflict`
- **Response**: PaymentResponse object with existing transaction info
- **Message**: "Duplicate request - returning existing transaction"

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `400` | Bad Request | Missing/invalid required fields | Validation failed with field details |
| `400` | Bad Request | Negative/zero amount | "amount must be greater than 0" |
| `400` | Bad Request | Invalid currency format | "currency must be 3 characters" |
| `400` | Bad Request | Invalid payment method | "paymentMethod is required" |
| `400` | Bad Request | Invalid webhook URL | "webhookUrl must be HTTPS" |
| `409` | Conflict | Duplicate idempotency key | "Duplicate request - returning existing transaction" |
| `500` | Internal Server Error | Payment provider failure | "An unexpected error occurred" |

**Idempotency Handling:**
- The API uses the `idempotencyKey` field to detect duplicate requests
- If the same idempotency key is used within 24 hours, the original transaction is returned with `200 OK`
- This follows REST/HTTP idempotency best practices
- Useful for client retries on network failures

**Request Validation:**
```json
{
  "merchantId": "required, non-empty string",
  "amount": "required, BigDecimal > 0",
  "currency": "required, exactly 3 characters (e.g., 'USD')",
  "idempotencyKey": "required, UUID format recommended",
  "paymentMethod": "required, non-empty string",
  "webhookUrl": "required, HTTPS URL",
  "customerId": "optional, string",
  "metadata": "optional, JSON object"
}
```

---

### 2. GET /api/v1/payments/{transactionId} - Retrieve Payment

**Success Response:**
- **Status Code**: `200 OK`
- **Response**: PaymentTransaction object

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `404` | Not Found | Transaction ID doesn't exist | "PaymentTransaction not found with id : '<transactionId>'" |
| `500` | Internal Server Error | Database error | "An unexpected error occurred" |

---

### 3. POST /api/v1/refunds - Process Refund

**Success Response:**
- **Status Code**: `201 Created`
- **Response**: Refund object

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `400` | Bad Request | Missing/invalid required fields | Validation failed with field details |
| `400` | Bad Request | Refund amount ≤ 0 | "Refund amount must be greater than zero" |
| `400` | Bad Request | Refund exceeds balance | "Refund amount exceeds remaining amount. Requested: X, Available: Y" |
| `400` | Bad Request | Invalid transaction status | "Transaction cannot be refunded. Current status: <status>" |
| `404` | Not Found | Transaction doesn't exist | "PaymentTransaction not found with id : '<transactionId>'" |
| `500` | Internal Server Error | Refund provider failure | "An unexpected error occurred" |

**Request Validation:**
```json
{
  "transactionId": "required, valid UUID",
  "amount": "required, BigDecimal > 0",
  "reason": "optional, string"
}
```

**Valid Transaction Statuses for Refund:**
- `SUCCESS` - Can be fully or partially refunded
- `PARTIAL_REFUND` - Can be further refunded up to original amount

**Invalid Transaction Statuses for Refund:**
- `PENDING`, `PROCESSING`, `FAILED`, `REFUNDED`

---

### 4. POST /api/v1/webhooks/test - Receive Test Webhook

**Success Response:**
- **Status Code**: `200 OK`
- **Response**: `{"status": "received", "message": "Webhook received successfully"}`

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `400` | Bad Request | Empty payload | "Webhook payload cannot be empty" |
| `500` | Internal Server Error | Storage failure | "An unexpected error occurred" |

---

### 5. GET /api/v1/webhooks/test/history - Retrieve Webhook History

**Success Response:**
- **Status Code**: `200 OK`
- **Response**: 
```json
{
  "totalReceived": 10,
  "events": [
    {"receivedAt": "...", "payload": "..."},
    ...
  ]
}
```

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `500` | Internal Server Error | System error | "An unexpected error occurred" |

---

### 6. DELETE /api/v1/webhooks/test/history - Clear Webhook History

**Success Response:**
- **Status Code**: `204 No Content`
- **Response**: Empty body

**Error Scenarios:**

| HTTP Code | Error | Condition | Message |
|-----------|-------|-----------|---------|
| `500` | Internal Server Error | System error | "An unexpected error occurred" |

---

## Exception Types

### 1. PaymentGatewayException
Custom exception for business logic errors.

**Attributes:**
- `httpStatus`: Appropriate HTTP status code
- `errorCode`: Machine-readable error code
- `message`: Human-readable error message

**Example:**
```java
throw new PaymentGatewayException(
    "Transaction cannot be refunded. Current status: PENDING",
    HttpStatus.BAD_REQUEST,
    "INVALID_TRANSACTION_STATUS"
);
```

### 2. ResourceNotFoundException
Extends `PaymentGatewayException` for resource lookup failures.

**HTTP Status**: `404 Not Found`

**Example:**
```java
throw new ResourceNotFoundException("PaymentTransaction", "id", transactionId);
```

### 3. IdempotentDuplicateException
Extends `PaymentGatewayException` for duplicate requests detected by idempotency key.

**HTTP Status**: `200 OK` (special case - not an error, but a duplicate)

**Example:**
```java
throw new IdempotentDuplicateException(transactionId, 
    "Duplicate request - returning existing transaction");
```

**Note:** Returns `409 Conflict` instead of `201 Created`. This follows REST best practices for idempotent operations - duplicate requests with the same idempotency key return conflict status with the existing transaction.

### 4. MethodArgumentNotValidException
Spring-handled validation errors from `@Valid` annotations.

**HTTP Status**: `400 Bad Request`

**Response includes:**
- Individual field validation errors
- Global object validation errors

### 5. IllegalArgumentException
Used for invalid input parameters.

**HTTP Status**: `400 Bad Request`

### 6. General Exception
All other unexpected exceptions.

**HTTP Status**: `500 Internal Server Error`
**Message**: "An unexpected error occurred"

---

## Error Codes Reference

| Error Code | HTTP Status | Meaning |
|-----------|-----------|---------|
| `IDEMPOTENT_DUPLICATE` | 409 | Duplicate request detected (existing transaction returned) |
| `RESOURCE_NOT_FOUND` | 404 | Resource lookup failed |
| `INVALID_TRANSACTION_STATUS` | 400 | Transaction in invalid state for operation |
| `INVALID_REFUND_AMOUNT` | 400 | Refund amount is invalid (≤ 0) |
| `REFUND_AMOUNT_EXCEEDS_BALANCE` | 400 | Refund exceeds remaining balance |
| `PAYMENT_GATEWAY_ERROR` | 400+ | Generic payment gateway error |

---

## Best Practices for Error Handling

### For API Consumers:

1. **Always check HTTP status code** to determine outcome
2. **Use traceId** for error reporting and investigation
3. **Parse details array** for validation errors
4. **Implement retry logic** for `5xx` errors (server errors)
5. **Do not retry** `4xx` errors without fixing the request

### For Developers:

1. **Log with context** - All exceptions are logged automatically
2. **Use specific exception types** - Don't throw generic `RuntimeException`
3. **Provide actionable messages** - Include what went wrong and how to fix it
4. **Include details** - For validation errors, always include field-level details
5. **Set appropriate HTTP status** - Choose correct status code for the error type

---

## Example Error Scenarios

### Validation Error (Missing Required Field)
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/payments",
  "timestamp": "2026-01-21T10:30:00",
  "details": [
    "amount: must not be null",
    "currency: must not be blank"
  ],
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Resource Not Found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "PaymentTransaction not found with id : '123e4567-e89b-12d3-a456-426614174000'",
  "path": "/api/v1/payments/123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-01-21T10:30:00",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Invalid Refund Amount
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Refund amount exceeds remaining amount. Requested: 1000, Available: 500",
  "path": "/api/v1/refunds",
  "timestamp": "2026-01-21T10:30:00",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Refund Invalid Transaction Status
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Transaction cannot be refunded. Current status: PENDING",
  "path": "/api/v1/refunds",
  "timestamp": "2026-01-21T10:30:00",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Testing Error Scenarios

### Test Missing Fields
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"merchantId": "test"}'
```
**Expected**: 400 Bad Request with validation errors

### Test Non-Existent Transaction
```bash
curl http://localhost:8080/api/v1/payments/invalid-id
```
**Expected**: 404 Not Found

### Test Refund Exceeding Balance
```bash
curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Content-Type: application/json" \
  -d '{"transactionId": "tx-id", "amount": 99999}'
```
**Expected**: 400 Bad Request with balance validation error

### Test Empty Webhook Payload
```bash
curl -X POST http://localhost:8080/api/v1/webhooks/test \
  -H "Content-Type: application/json" \
  -d ''
```
**Expected**: 400 Bad Request
