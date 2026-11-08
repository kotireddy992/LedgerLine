# PROMPTS.md — AI Usage Log

This file records all AI assistance used during LedgerLine development.

---

## Entry 1

**Date:** 2025-07-14  
**Prompt Used:** Full master prompt — "You are a senior Java backend engineer... Build LedgerLine..."  
**AI-Generated Result:** Complete project scaffold including pom.xml, all entities, repositories, services, controllers, security configuration, Flyway migrations, integration tests, and documentation files.  
**What Was Incorrect or Incomplete:**  
- `AuthDtos.java` initially used inner classes which do not work with Spring's `@Valid` annotation. Replaced with separate DTO files.  
- `StatementService` initially called `assertOwnerOrAdmin` which rejected AUDITOR role. Fixed to explicitly check for AUDITOR.  
- `TransferServiceTest.transfer_frozenAccount_throwsInvalidOperation` uses `doCallRealMethod()` on a mock which requires the real `AccountService` bean — this test may need adjustment if `AccountService` is not a concrete class in the mock context. Noted for verification.  
**How I Verified It:** Reviewed each file for compilation correctness, checked transaction boundaries, verified lock ordering logic, checked that no DTO exposes `passwordHash`.  
**What I Changed:**  
- Replaced `AuthDtos.java` inner classes with separate files.  
- Fixed `StatementService` to allow AUDITOR access.  
- Replaced `Spring Boot 4.x` (which does not exist) with `3.3.5` in `pom.xml`.  
**Relevant Git Commit:** (to be filled after actual commit)

---

## Notes on AI Verification

The following were manually verified before accepting AI output:

1. **Transaction boundaries:** `@Transactional` on all financial operations. `IdempotencyService` uses `REQUIRES_NEW` so the idempotency INSERT commits independently.
2. **Lock ordering:** `Math.min/max` on account IDs confirmed to prevent deadlock.
3. **No negative balance:** Balance check is after `findAccountWithLockOrThrow`, not before. Correct.
4. **No password in responses:** All DTOs checked — `passwordHash` field absent from all response types.
5. **JWT secret not logged:** `JwtService` constructor takes the secret as a parameter; no logging statement present.
6. **Flyway migrations:** V1 creates all tables. V2 seeds admin. `spring.jpa.hibernate.ddl-auto=validate` ensures Hibernate does not alter the schema.
7. **BigDecimal precision:** All monetary fields use `DECIMAL(19,2)` in SQL and `BigDecimal` in Java. No `float` or `double` anywhere.
