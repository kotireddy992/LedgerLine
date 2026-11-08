# THREATS.md — LedgerLine Security Threat Model

---

## 1. JWT Secret Exposure

**Threat:** JWT signing secret leaked via logs, config files, or version control.  
**Attack Scenario:** Attacker reads the secret and forges tokens for any user or role.  
**Impact:** Complete authentication bypass. Full system compromise.  
**Mitigation:** Secret loaded from environment variable only (`${JWT_SECRET}`). Never logged. Not in `application.properties` default for production. `.gitignore` excludes `.env` files.  
**Test Verifying Mitigation:** `AuthIntegrationTest.protectedEndpoint_withoutToken_returns401` — a forged token without the correct secret is rejected.  
**Remaining Risk:** If the environment variable is leaked (e.g., via a cloud metadata endpoint), all issued tokens are compromised. Mitigation: rotate the secret and invalidate all tokens (requires a token blacklist, not currently implemented).

---

## 2. Horizontal Privilege Escalation (Account Access)

**Threat:** User A accesses or modifies User B's account by guessing account IDs.  
**Attack Scenario:** User A sends `GET /api/accounts/2/balance` where account 2 belongs to User B.  
**Impact:** Unauthorized balance disclosure or money movement.  
**Mitigation:** `AccountService.assertOwnerOrAdmin()` checks the authenticated user (from SecurityContext, never from request) against the account owner on every request.  
**Test Verifying Mitigation:** `FinancialIntegrationTest.deposit_unauthorizedAccount_returns403`, `TransferIntegrationTest.transfer_unauthorizedUser_cannotTransferOthersAccount`.  
**Remaining Risk:** None for current implementation. Admin can access all accounts by design.

---

## 3. Negative Balance via Race Condition

**Threat:** Two concurrent withdrawals both read the same balance and both succeed, producing a negative balance.  
**Attack Scenario:** Balance = 100. Thread A and Thread B both read 100, both check 100 >= 100, both deduct 100. Final balance = -100.  
**Impact:** Financial loss. System integrity violation.  
**Mitigation:** Pessimistic row-level locking (`SELECT FOR UPDATE` via `@Lock(PESSIMISTIC_WRITE)`). The second thread blocks until the first commits. It then reads the updated balance (0) and correctly rejects the withdrawal.  
**Test Verifying Mitigation:** `ConcurrencyIntegrationTest.concurrentWithdrawals_neverProduceNegativeBalance` — 10 concurrent withdrawals of 100 from a 500-balance account. Exactly 5 succeed, final balance is 0.00.  
**Remaining Risk:** If the application is deployed across multiple JVMs without a shared database, Java-level synchronization would be insufficient. The DB-level lock handles this correctly.

---

## 4. Deadlock on Opposite-Direction Transfers

**Threat:** Thread A locks account 1 then waits for account 2. Thread B locks account 2 then waits for account 1. Circular wait → deadlock.  
**Attack Scenario:** Simultaneous transfers: A→B and B→A.  
**Impact:** Both transactions hang indefinitely. Database timeout errors. Service unavailability.  
**Mitigation:** Deterministic lock ordering — always lock the account with the lower ID first. Both threads attempt to lock the same account first. One succeeds; the other blocks. No circular wait.  
**Test Verifying Mitigation:** `ConcurrencyIntegrationTest.concurrentTransfers_oppositeDirection_noDeadlock`.  
**Remaining Risk:** None for two-account transfers. For transfers involving more than two accounts in a chain, the ordering must be extended.

---

## 5. Duplicate Money Movement (Idempotency Failure)

**Threat:** Network retry causes the same transfer to execute twice.  
**Attack Scenario:** Client sends a transfer, receives a timeout, retries with the same request. Server executes the transfer twice.  
**Impact:** Double debit from source account. Financial loss.  
**Mitigation:** DB-backed idempotency with a unique constraint on `idempotency_key`. The second request finds the existing record and returns the cached result without executing the financial operation.  
**Test Verifying Mitigation:** `TransferIntegrationTest.transfer_idempotency_sameKeyTwice_moneyMovesOnce`, `FinancialIntegrationTest.deposit_idempotency_sameKeyTwice_moneyMovesOnce`.  
**Remaining Risk:** If the application crashes between inserting the PROCESSING record and marking it COMPLETED, the key is stuck in PROCESSING. A background cleanup job (not implemented) should expire stale PROCESSING records after a configurable timeout.

---

## 6. Password Hash Exposure

**Threat:** Password hash returned in API response or logged.  
**Attack Scenario:** Attacker reads the hash from an API response and performs offline brute-force.  
**Impact:** Account takeover.  
**Mitigation:** `UserResponse` DTO does not include `passwordHash`. `User` entity's `passwordHash` field is never mapped to any response DTO. BCrypt cost=12 makes brute-force computationally expensive.  
**Test Verifying Mitigation:** `AuthServiceTest.register_success` verifies the returned `UserResponse` does not contain a password field.  
**Remaining Risk:** BCrypt cost=12 is appropriate for 2024 hardware. Should be increased as hardware improves.

---

## 7. SQL Injection

**Threat:** Attacker injects SQL via request parameters.  
**Attack Scenario:** `GET /api/accounts?id=1 OR 1=1`  
**Impact:** Unauthorized data access or modification.  
**Mitigation:** All database access uses JPA/Hibernate with parameterized queries. No raw SQL string concatenation. JPQL named parameters used in custom queries.  
**Test Verifying Mitigation:** Covered implicitly by all integration tests — no raw SQL is used.  
**Remaining Risk:** None for current implementation.

---

## 8. Stale PROCESSING Idempotency Records

**Threat:** Application crashes mid-transfer, leaving an idempotency key in PROCESSING state permanently.  
**Attack Scenario:** Client retries the request. Server sees PROCESSING and returns 409 forever. The transfer never completes or fails cleanly.  
**Impact:** Stuck transactions. Poor user experience.  
**Mitigation:** Documented as a known limitation. A production system should implement a background job that expires PROCESSING records older than a configurable timeout (e.g., 5 minutes) and marks them FAILED.  
**Remaining Risk:** Present in current implementation. Acceptable for a challenge submission; must be addressed before production deployment.

---

## 9. Auditor Write Access

**Threat:** AUDITOR role modifies financial data.  
**Attack Scenario:** Auditor sends `POST /api/accounts/{id}/deposit`.  
**Impact:** Unauthorized money movement.  
**Mitigation:** `SecurityConfig` restricts `/api/accounts/**` deposit/withdraw to authenticated users, and `AccountService.assertOwnerOrAdmin` rejects non-owners. AUDITOR is not ADMIN and does not own accounts. Auditor endpoints are read-only by design.  
**Test Verifying Mitigation:** Role-based access enforced at SecurityConfig level. Any AUDITOR attempting a deposit will receive 403.  
**Remaining Risk:** None.
