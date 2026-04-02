# Coupon API

REST API for managing discount coupons, built with Spring Boot 3.

## Tech Stack

- Java 17
- Spring Boot 3.2.4 (Web, Data JPA, Validation)
- H2 (in-memory database)
- Hibernate 6 with `@SQLRestriction` for soft delete filtering
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito
- Docker + Docker Compose

## Running Locally

**Prerequisites:** Java 17, Maven 3.x

```bash
./mvnw spring-boot:run
```

App available at `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:coupondb`)

## Running with Docker

```bash
./mvnw clean package -DskipTests
docker-compose up --build
```

## Running Tests

```bash
./mvnw test
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/coupon` | Create a new coupon |
| `DELETE` | `/coupon/{id}` | Soft delete a coupon |

### POST /coupon

**Request body:**
```json
{
  "code": "ABC123",
  "description": "10% off your order",
  "discountValue": 10.0,
  "expirationDate": "2026-12-31",
  "published": false
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "code": "ABC123",
  "description": "10% off your order",
  "discountValue": 10.0,
  "expirationDate": "2026-12-31",
  "published": false,
  "redeemed": false,
  "status": "INACTIVE"
}
```

### DELETE /coupon/{id}

Returns `204 No Content` on success.

## Business Rules

- **Code:** Alphanumeric only, exactly 6 characters. Special characters are stripped automatically before saving.
- **Discount:** Minimum value of `0.5`, no upper limit.
- **Expiration date:** Cannot be in the past.
- **Published:** Optional field (defaults to `false`). Determines if status is `ACTIVE` or `INACTIVE`.
- **Soft delete:** The `deleted` field is set to `true`; the record is never physically removed. Deleting an already-deleted coupon returns `422`.

## Status Values

| Status | Condition |
|--------|-----------|
| `ACTIVE` | `published = true` and not deleted |
| `INACTIVE` | `published = false` and not deleted |
| `DELETED` | `deleted = true` |

## Architectural Decisions

- **Domain encapsulation:** All business rules live in `Coupon.create()` and `Coupon.delete()` — the service layer is intentionally thin.
- **`@SQLRestriction("deleted = false")`:** Hibernate 6 annotation that automatically filters deleted coupons from all standard queries. A native query (`findByIdIncludingDeleted`) bypasses this filter for the delete operation, allowing the API to distinguish "not found" from "already deleted".
- **Stateless DTOs:** `CreateCouponRequest` (input) and `CouponResponse` (output) are Java records, keeping the domain entity fully decoupled from the API contract.
- **Global error handling:** `GlobalExceptionHandler` maps domain exceptions to standardized `ApiError` responses with consistent structure.
