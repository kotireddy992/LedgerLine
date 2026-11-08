# LedgerLine — Money Movement with a Ledger You Can Prove

Snovasys Fresher Hiring Challenge 2026 — VVIT

---

## Project Overview

LedgerLine is a production-style financial backend that implements:
- Double-entry bookkeeping ledger
- Atomic money transfers with deadlock prevention
- DB-backed idempotency (no in-memory maps)
- Pessimistic row-level locking for concurrency safety
- Role-based access control (ADMIN, ACCOUNT_HOLDER, AUDITOR)
- JWT authentication with BCrypt password hashing
- Flyway database migrations
- Comprehensive integration tests using Testcontainers + real MySQL

---

## Architecture

```
Controller → Service → Repository → MySQL (via JPA/Hibernate)

com.ledgerline
├── LedgerLineApplication       Entry point
├── config/                     SecurityConfig, OpenApiConfig
├── security/                   JwtService, JwtAuthenticationFilter, SecurityUtils, UserDetailsServiceImpl
├── controller/                 AuthController, AccountController, TransferController, AdminController, AuditController
├── service/                    AuthService, AccountService, DepositWithdrawalService, TransferService,
│                               LedgerService, StatementService, AdminService, IdempotencyService
├── repository/                 UserRepository, AccountRepository, TransferRepository,
│                               LedgerEntryRepository, IdempotencyRecordRepository
├── entity/                     User, Account, Transfer, LedgerEntry, IdempotencyRecord
├── dto/                        RegisterRequest, LoginRequest, AuthResponse, AccountResponse,
│                               MoneyRequest, TransferRequest, TransferResponse, LedgerEntryResponse,
│                               UserResponse, ErrorResponse, CreateAccountRequest
├── exception/                  GlobalExceptionHandler, ResourceNotFoundException, UnauthorizedException,
│                               InsufficientFundsException, InvalidOperationException, IdempotencyConflictException
├── enums/                      Role, AccountStatus, TransferStatus, EntryType
└── mapper/                     EntityMapper
```

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven |
| Database | MySQL 8 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Password Hashing | BCrypt (cost=12) |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Containerization | Docker + Docker Compose |

---

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 (or Docker)
- Docker + Docker Compose (optional, for containerized run)

---

## Database Configuration

The application connects to MySQL using environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/ledgerline_db?createDatabaseIfNotExist=true&...` | JDBC URL |
| `DB_USER` | `root` | Database username |
| `DB_PASS` | `root` | Database password |
| `JWT_SECRET` | (64-char default) | JWT signing secret — **MUST be changed in production** |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiry in milliseconds (24 hours) |

---

## Local Setup

### Option 1: Run with Docker Compose (recommended)

```bash
cd LedgerLine
docker-compose up --build
```

This starts MySQL 8 and the application. Flyway migrations run automatically.

### Option 2: Run with local MySQL

1. Create the database:
```sql
CREATE DATABASE ledgerline_db;
```

2. Set environment variables (or edit `application.properties`):
```bash
set DB_USER=root
set DB_PASS=your_password
```

3. Build and run:
```bash
mvn clean package -DskipTests
java -jar target/ledgerline-1.0.0.jar
```

---

## Running Tests

Integration tests require Docker (Testcontainers pulls MySQL automatically):

```bash
# All tests
mvn test

# Unit tests only (no Docker needed)
mvn test -Dtest="**/unit/**"

# Integration tests only
mvn test -Dtest="**/integration/**"
```

---

## Seed Users

Flyway V2 seeds one ADMIN user:

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@ledgerline.com | Admin@1234 |

Register additional ACCOUNT_HOLDER and AUDITOR users via `POST /api/auth/register`.

**Change the admin password immediately after first login in production.**

---

## API Documentation

Swagger UI: http://localhost:8080/swagger-ui.html  
OpenAPI JSON: http://localhost:8080/v3/api-docs

### Authentication

| Method | URL | Role | Changes Money |
|---|---|---|---|
| POST | /api/auth/register | Public | No |
| POST | /api/auth/login | Public | No |

### Accounts

| Method | URL | Role | Changes Money |
|---|---|---|---|
| POST | /api/accounts | ACCOUNT_HOLDER, ADMIN | No |
| GET | /api/accounts | ACCOUNT_HOLDER (own), ADMIN | No |
| GET | /api/accounts/{id} | Owner, ADMIN | No |
| GET | /api/accounts/{id}/balance | Owner, ADMIN | No |
| POST | /api/accounts/{id}/deposit | Owner, ADMIN | **YES** |
| POST | /api/accounts/{id}/withdraw | Owner, ADMIN | **YES** |
| GET | /api/accounts/{id}/statement | Owner, ADMIN, AUDITOR | No |

### Transfers

| Method | URL | Role | Changes Money |
|---|---|---|---|
| POST | /api/transfers | Source owner, ADMIN | **YES** |
| GET | /api/transfers/{id} | Source owner, ADMIN | No |

### Admin (ADMIN only)

| Method | URL | Role | Changes Money |
|---|---|---|---|
| GET | /api/admin/users | ADMIN | No |
| GET | /api/admin/accounts | ADMIN | No |
| GET | /api/admin/audit | ADMIN | No |

### Audit (ADMIN + AUDITOR)

| Method | URL | Role | Changes Money |
|---|---|---|---|
| GET | /api/audit/transactions | ADMIN, AUDITOR | No |
| GET | /api/audit/accounts/{id}/statement | ADMIN, AUDITOR | No |

---

## Idempotency

All money-moving endpoints accept an `Idempotency-Key` header:

```
Idempotency-Key: <your-unique-uuid>
```

- If omitted, a random key is generated (no replay protection).
- If the same key is submitted twice with the same payload, the original result is returned and money moves only once.
- If the same key is submitted with a different payload, HTTP 409 is returned.

---

## Financial Integrity

### Double-Entry Invariant
For every completed internal transfer:
- One DEBIT entry on the source account
- One CREDIT entry on the destination account
- DEBIT amount = CREDIT amount
- Net change to total system balance = 0

### No Negative Balances
Balance checks are performed after acquiring a pessimistic row lock. No concurrent withdrawal can read a stale balance.

### Monetary Precision
All amounts stored as `DECIMAL(19,2)` with `RoundingMode.HALF_EVEN` (banker's rounding).

---

## Deployment

### Environment Variables for Production

```bash
DB_URL=jdbc:mysql://<host>:3306/ledgerline_db?useSSL=true&serverTimezone=UTC
DB_USER=<db_user>
DB_PASS=<strong_password>
JWT_SECRET=<minimum_64_character_random_secret>
JWT_EXPIRATION_MS=3600000
```

Never commit secrets to version control.
