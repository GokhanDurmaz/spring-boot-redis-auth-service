# Payment Processing REST API - Comprehensive Documentation

## Overview

This is a comprehensive REST API for payment processing with Stripe integration. The system allows merchants to set up stores and accept payments from customers using multiple payment methods.

## Key Features

- **Multi-tenant Merchant Model**: Merchants can create and manage stores
- **Multiple Payment Methods**: Support for cards, bank transfers, and digital wallets via Stripe
- **Complete Payment Lifecycle**: Create, retrieve, refund, and cancel payments
- **Robust Error Handling**: Exponential backoff retries for failed payments
- **Event-Driven Architecture**: Internal events for payment state changes
- **Webhook Integration**: Real-time payment confirmations via Stripe webhooks
- **Comprehensive Analytics**: Track payment metrics and success rates
- **Authorization Checks**: Merchants can only access their own data

## Technology Stack

- **Framework**: Spring Boot 4.1.1
- **Language**: Java 17
- **Database**: PostgreSQL
- **Payment Provider**: Stripe
- **ORM**: JPA/Hibernate
- **Security**: Spring Security with JWT

## Environment Variables Required

```bash
STRIPE_API_KEY=sk_test_... # Stripe test API key
STRIPE_WEBHOOK_SECRET=whsec_... # Stripe webhook signing secret
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
```

## API Endpoints

### Merchant Management

#### Register Merchant
```
POST /api/v1/merchants
Content-Type: application/json

{
  "businessName": "Acme Corp",
  "businessEmail": "merchant@acme.com",
  "businessPhone": "+1234567890",
  "businessAddress": "123 Main St",
  "businessCity": "New York",
  "businessState": "NY",
  "businessZip": "10001",
  "businessCountry": "US",
  "businessDescription": "Online store",
  "taxId": "12-3456789"
}

Response: 201 Created
{
  "id": 1,
  "userId": 1,
  "businessName": "Acme Corp",
  "status": "PENDING_VERIFICATION",
  "stripeAccountId": "acct_...",
  "createdAt": "2026-09-25T10:00:00",
  "updatedAt": "2026-09-25T10:00:00"
}
```

#### Get Merchant
```
GET /api/v1/merchants/{merchantId}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "businessName": "Acme Corp",
  "status": "ACTIVE",
  ...
}
```

### Store Management

#### Create Store
```
POST /api/v1/merchants/{merchantId}/stores
Authorization: Bearer {token}
Content-Type: application/json

{
  "storeName": "Online Store",
  "storeDescription": "E-commerce store",
  "storeEmail": "store@acme.com",
  "storePhone": "+1234567890",
  "logoUrl": "https://...",
  "websiteUrl": "https://acme.com",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zip": "10001",
  "country": "US"
}

Response: 201 Created
{
  "id": 1,
  "merchantId": 1,
  "storeName": "Online Store",
  "status": "ACTIVE",
  "createdAt": "2026-09-25T10:00:00"
}
```

#### List Stores
```
GET /api/v1/merchants/{merchantId}/stores
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "storeName": "Online Store",
    "status": "ACTIVE"
  }
]
```

### Payment Methods

#### Add Payment Method
```
POST /api/v1/payment-methods
Authorization: Bearer {token}
Content-Type: application/json

{
  "type": "CARD",
  "stripeToken": "pm_...",
  "cardBrand": "visa",
  "cardLast4": "4242",
  "cardExpiryMonth": 12,
  "cardExpiryYear": 2026,
  "isDefault": true
}

Response: 201 Created
{
  "id": 1,
  "type": "CARD",
  "cardBrand": "visa",
  "cardLast4": "4242",
  "isDefault": true,
  "isActive": true,
  "createdAt": "2026-09-25T10:00:00"
}
```

#### List Payment Methods
```
GET /api/v1/payment-methods
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "type": "CARD",
    "cardLast4": "4242",
    "isDefault": true
  }
]
```

### Payments

#### Create Payment
```
POST /api/v1/payments
Authorization: Bearer {token}
Content-Type: application/json

{
  "storeId": 1,
  "amount": 99.99,
  "currency": "USD",
  "paymentMethodId": 1,
  "description": "Order #12345"
}

Response: 201 Created
{
  "id": 1,
  "storeId": 1,
  "customerId": 1,
  "amount": 99.99,
  "currency": "USD",
  "status": "PENDING",
  "stripePaymentIntentId": "pi_...",
  "createdAt": "2026-09-25T10:00:00"
}
```

#### Get Payment
```
GET /api/v1/payments/{paymentId}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "amount": 99.99,
  "status": "SUCCEEDED",
  "succeededAt": "2026-09-25T10:01:00"
}
```

#### List Payments for Store
```
GET /api/v1/payments/store/{storeId}?page=0&size=10&status=SUCCEEDED
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "amount": 99.99,
      "status": "SUCCEEDED"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

#### List Payments for Customer
```
GET /api/v1/payments/customer/{customerId}?page=0&size=10&status=SUCCEEDED
Authorization: Bearer {token}

Response: 200 OK
[Similar structure to store payments]
```

### Refunds

#### Create Refund
```
POST /api/v1/payments/{paymentId}/refunds
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 50.00,
  "reason": "REQUESTED_BY_CUSTOMER",
  "metadata": "Partial refund for order #12345"
}

Response: 201 Created
{
  "id": 1,
  "paymentId": 1,
  "amount": 50.00,
  "status": "SUCCEEDED",
  "reason": "REQUESTED_BY_CUSTOMER",
  "refundedAt": "2026-09-25T10:02:00"
}
```

#### Get Refunds for Payment
```
GET /api/v1/payments/{paymentId}/refunds
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "amount": 50.00,
    "status": "SUCCEEDED"
  }
]
```

#### Cancel Payment
```
POST /api/v1/payments/{paymentId}/cancel
Authorization: Bearer {token}

Response: 204 No Content
```

### Analytics

#### Get Payment Analytics
```
GET /api/v1/analytics/payments
Authorization: Bearer {token}

Response: 200 OK
{
  "totalPaymentsCreated": 100,
  "totalPaymentsSucceeded": 95,
  "totalPaymentsFailed": 5,
  "totalPaymentsRefunded": 2,
  "totalAmountProcessed": 9999.99,
  "totalAmountRefunded": 100.00,
  "successRate": 95.0
}
```

### Webhooks

#### Stripe Webhook Handler
```
POST /api/v1/webhooks/stripe
Content-Type: application/json
Stripe-Signature: t=...,v1=...

{
  "id": "evt_...",
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_...",
      "status": "succeeded"
    }
  }
}

Response: 200 OK
```

## Error Responses

All error responses follow this format:

```json
{
  "status": 400,
  "error": "Validation Error",
  "message": "Request validation failed",
  "timestamp": "2026-09-25T10:00:00",
  "path": "/api/v1/payments",
  "fieldErrors": [
    {
      "field": "amount",
      "message": "Amount must be positive",
      "rejectedValue": -10
    }
  ]
}
```

## HTTP Status Codes

- **200 OK**: Successful GET request
- **201 Created**: Successful POST request
- **204 No Content**: Successful DELETE/cancel request
- **400 Bad Request**: Validation error or invalid payment operation
- **404 Not Found**: Resource not found
- **409 Conflict**: Invalid payment state
- **500 Internal Server Error**: Server error

## Payment Lifecycle

1. **PENDING**: Payment created, waiting for processing
2. **PROCESSING**: Payment in progress
3. **SUCCEEDED**: Payment successful
4. **FAILED**: Payment failed (will be retried automatically)
5. **CANCELLED**: Payment cancelled by merchant
6. **REFUNDED**: Payment fully refunded
7. **PARTIAL_REFUNDED**: Payment partially refunded

## Retry Logic

Failed payments are automatically retried using exponential backoff:
- Max retries: 5 attempts
- Initial delay: 1 second
- Backoff formula: min(1s * 2^attempt, 300s)
- Retry schedule: 1s, 2s, 4s, 8s, 16s

Retries happen automatically every 30 seconds.

## Security

- All endpoints require JWT authentication (except webhooks)
- Authorization checks ensure users can only access their own data
- Merchants can only manage their own stores and payments
- Customers can only access their own payment methods and transactions
- Stripe webhook signatures are verified for authenticity

## Building and Running

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test
```

## Configuration

Key application properties (in application.properties):

```properties
# Stripe
stripe.api.key=${STRIPE_API_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}

# Payment Retry
payment.retry.max-attempts=5
payment.retry.initial-delay-seconds=1
payment.retry.max-delay-seconds=300

# Database
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## Architecture

The system follows a layered architecture:

- **Controllers**: REST endpoints
- **Services**: Business logic
- **Repositories**: Data access
- **Entities**: Domain models
- **DTOs**: Request/Response objects
- **Event Listeners**: Async processing
- **Exception Handlers**: Centralized error handling

## Features Implemented

✓ Multi-tenant merchant model
✓ Store management
✓ Payment method management (cards, bank accounts, digital wallets)
✓ Payment creation with Stripe integration
✓ Webhook handling for payment confirmations
✓ Refunds and payment cancellations
✓ Exponential backoff retry logic
✓ Event-driven architecture
✓ Comprehensive error handling
✓ Analytics and metrics
✓ Pagination and filtering
✓ Authorization checks

## Future Enhancements

- Invoice generation
- Subscription payments
- Payment splitting
- Multi-currency support
- Advanced fraud detection
- Settlement tracking
- Reconciliation tools


## Integration Testing

### Overview

Comprehensive end-to-end integration tests are provided in `src/test/java/com/example/demo_app/integration/PaymentApiIntegrationTest.java`. These tests cover the complete payment workflow and validate all critical API functionality.

### Test Coverage

The integration test suite includes:

#### 1. **Merchant Management Tests**
- `testMerchantCreation()` - Validates merchant account creation
- `testGetMerchantDetails()` - Verifies merchant data retrieval

#### 2. **Store Management Tests**
- `testCreateStore()` - Tests store creation under a merchant
- `testGetStoresForMerchant()` - Validates listing stores for a merchant

#### 3. **Payment Method Management Tests**
- `testAddPaymentMethod()` - Tests adding cards/bank accounts to a merchant
- `testGetPaymentMethods()` - Retrieves payment methods for a merchant
- `testDeletePaymentMethod()` - Removes a payment method

#### 4. **Payment Lifecycle Tests**
- `testCreatePayment()` - Creates a new payment
- `testGetPaymentDetails()` - Retrieves payment information
- `testListPayments()` - Lists all payments for merchant
- `testRefundPayment()` - Processes a full refund
- `testPartialRefund()` - Processes a partial refund
- `testCancelPayment()` - Cancels a pending payment
- `testCannotRefundCancelledPayment()` - Validates business rules (conflict handling)

#### 5. **Authorization & Security Tests**
- `testCrossMerchantPaymentAccessDenied()` - Ensures cross-merchant isolation
- `testCrossMerchantRefundDenied()` - Validates authorization on refund operations
- `testMissingAuthorizationToken()` - Tests unauthenticated requests
- `testInvalidPaymentAmount()` - Validates input constraints

#### 6. **Error Handling Tests**
- `testNonexistentPaymentReturns404()` - 404 handling
- `testInvalidPaymentAmount()` - Validation error handling

#### 7. **Complete Workflow Test**
- `testCompletePaymentWorkflow()` - End-to-end scenario:
  1. Merchant registration
  2. Store creation
  3. Payment method addition
  4. Payment creation
  5. Webhook simulation (payment confirmation)
  6. Refund processing

### Running Tests

#### Prerequisites

Before running tests, ensure:
1. Java 17+ is installed
2. PostgreSQL is running (or H2 will be used for tests)
3. No services need to be running - tests use in-memory H2 database

#### Run All Tests

```bash
./gradlew test
```

#### Run Specific Test

```bash
./gradlew test --tests PaymentApiIntegrationTest.testCompletePaymentWorkflow
```

#### Run Specific Test Class

```bash
./gradlew test --tests PaymentApiIntegrationTest
```

#### Run with Detailed Output

```bash
./gradlew test -i --tests PaymentApiIntegrationTest
```

### Test Configuration

Tests use the `application-test.yml` configuration which includes:

- **Database**: In-memory H2 database (auto-configured)
- **Cache**: Simple in-memory cache
- **Redis**: Disabled for tests
- **JWT Secret**: Test secret included
- **Stripe Keys**: Test API keys included

### Test Data Setup

The `@BeforeEach` method in each test:

1. Cleans up all test repositories
2. Creates test merchants with unique IDs
3. Sets up JWT tokens for authenticated requests
4. Creates test stores and payment methods
5. Initializes MockMvc for HTTP testing

### Key Test Assertions

Tests validate:

- HTTP status codes (201, 200, 400, 404, 409, 403)
- Response body content using JSONPath
- Entity persistence in the database
- Authorization and access control
- Business logic validation (e.g., cannot refund cancelled payments)
- Error message format and content

### Example Test Execution

```bash
# Run the complete workflow test
./gradlew test --tests PaymentApiIntegrationTest.testCompletePaymentWorkflow

# Expected output:
# - 1 payment created successfully
# - Payment retrieved with correct amount/currency
# - Payment status updated to SUCCEEDED
# - Refund created for full amount
# - All assertions pass ✓
```

### Test Dependencies

The test suite uses:

- **Spring Boot Test**: Integration testing framework
- **Spring Security Test**: Authorization testing utilities
- **Hamcrest**: Matcher library for assertions
- **H2 Database**: In-memory relational database
- **JUnit 5**: Test runner

### Troubleshooting Tests

#### Redis Connection Errors

If tests fail with Redis connection errors:
1. Ensure `application-test.yml` excludes Redis autoconfiguration
2. Tests should not attempt to connect to external services

#### Database Issues

If H2 database fails to initialize:
1. Check that the H2 driver is on the classpath
2. Verify `application-test.yml` datasource URL is correct
3. Ensure JPA dialect is set to `org.hibernate.dialect.H2Dialect`

#### JWT Token Issues

If authentication fails in tests:
1. Verify `app.jwt.secret` is set in `application-test.yml`
2. Check that `JwtUtil` is properly wired in test configuration
3. Ensure tokens are included in `Authorization: Bearer {token}` header

### Continuous Integration

To run tests in CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Integration Tests
  run: ./gradlew test --tests PaymentApiIntegrationTest
  
# Example GitLab CI
test:
  script:
    - ./gradlew test --tests PaymentApiIntegrationTest
```

### Performance Considerations

- Each test runs in **~500ms to 2s** (excluding setup)
- Full test suite completes in **~20-30s**
- H2 in-memory database ensures fast test execution
- Tests can run in parallel with `-x` gradle option

### Future Testing Enhancements

Potential improvements:
1. Add performance/load testing with JMH
2. Add contract testing for Stripe API
3. Add mutation testing to verify test quality
4. Add integration with Testcontainers for external service testing
5. Add API documentation tests (Spring REST Docs)

