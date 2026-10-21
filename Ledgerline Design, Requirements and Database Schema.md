# Ledgerline: Design, Requirements and Database Schema

Oct 20, 2026 · @Naren

## Overview

Ledgerline is a Java and Spring Boot backend that moves simulated money between accounts and guarantees that money is never created, destroyed or duplicated, even under concurrent requests, retries and crashes.

**Problem.** A CRUD endpoint that does `balance = balance - x` loses money the moment two requests run at once or a client retries after a timeout. Payments systems solve this with a double-entry ledger, database-enforced rules, row locking and idempotent requests. Ledgerline implements those techniques and proves them with automated tests.

**Stack.** Java 21, Spring Boot 3, PostgreSQL 16, Flyway, JUnit 5 and Testcontainers, Docker Compose. Amounts are whole paise (`BIGINT`), never floating point.

The financial rules (account types, entry signs, overspend protection, failure behaviour and the seven invariants) are defined in the Ledgerline Financial Rules Spec. This document turns those rules into requirements, a design and a database schema.

## Sprint plan

Each sprint ends with something that runs, a demo you can show, and passing tests. One week per sprint is assumed; stretch it to match the hours you actually have, but keep the sprint goals in this order.

**Working agreement: one vertical slice per sprint.** Each sprint delivers exactly one database table (a new Flyway migration), its entity and repository, the service/endpoint that uses it, and the tests for that slice — nothing else. The database schema grows one table at a time, in dependency order, instead of all six tables up front. Each table is an increment you can demo; the slice is done only when its tests pass against real PostgreSQL via Testcontainers.

| Sprint | Slice (table → entity → API → test) | Stories | Pts | Demo at the end |
| --- | --- | --- | --- | --- |
| 0 | Skeleton + `accounts`: migration, `Account` entity + repository, `POST/GET /accounts`, Testcontainers test | US-1, US-2 | 8 | Create an account over HTTP, then read it back; the DB migration runs on `docker compose up` |
| 1 | `ledger_entries` (+ minimal `transactions` parent): entities, repositories, repo tests | US-3 | 6 | Integration tests prove an entry inserts, links account + transaction, and rows are append-only |
| 2 | `transactions` completed (`type`, `status`, `reverses_transaction_id`): posting service, `POST/GET /transactions` | US-4, US-5 | 6 | Insert a transaction with two zero-sum entries; failed posts leave zero rows |
| 3 | Money enters and leaves: deposits and withdrawals on top of the posts slice | US-6, US-8 | 6 | Create an account, deposit, withdraw, and watch the paged ledger |
| 4 | Transfers between accounts, with rollback and deadlock-safe locking | US-7, US-9 | 7 | A transfer, a forced mid-transfer failure that rolls back, and the opposite-transfers test |
| 5 | `idempotency_keys`: entity, repository, key reuse in postings + retries | US-11, US-12, US-13 | 8 | 20 identical parallel requests, one transaction; the crash-after-commit test; key-misuse 422 |
| 6 | Concurrency hardening of the posting slice (tests only, no new tables) | US-10 | 5 | The 100-parallel-withdrawals test and the opposite-transfers test, both green, ten runs in a row |
| 7 | Reconciliation slice: reconciliation tables, service, admin endpoints | US-14, US-15 | 6 | Corrupt a balance, run reconciliation, see exactly that account reported |
| 8 | Release gate: invariants I1–I7, the 10,000-transfer gate, README, AC checklist | US-16 | 5 | Run the full acceptance suite from a clean checkout and show the README |

**Ceremonies, kept light for a solo project**

- **Sprint planning (15 minutes):** pick the sprint's stories, split any story you cannot finish in two days.
- **Daily check (5 minutes):** what is done, what is next, what is blocking. Write it in a running log in the repository.
- **Sprint review:** run the demo from a clean checkout; if it fails, the sprint is not done.
- **Retrospective (10 minutes):** record one thing to keep and one to change in `docs/decisions.md`. These notes become interview material.

**Backlog rules:** new ideas go to the bottom of the backlog, not into the current sprint. Stage 2 (outbox and workers) and Stage 3 (rate limiting, multi-instance, metrics) enter as new epics only after Sprint 5 is complete.

## Definition of Ready and Definition of Done

A story enters a sprint only when it is ready, and leaves it only when it is done; the release acceptance criteria below are the final gate for the whole of Stage 1.

**Definition of Ready**

- [ ] The story has a clear user, outcome and acceptance criteria.
- [ ] It can be finished in about two days; larger stories are split.
- [ ] Any schema or API change it needs is already in this document.

**Definition of Done**

- [ ] Acceptance criteria are met and demonstrated.
- [ ] Automated tests for the story pass on real PostgreSQL via Testcontainers.
- [ ] Money-path code was written test-first or has a test that fails when the logic is broken.
- [ ] No money-related invariant (I1 to I7) is weakened.
- [ ] The schema change, if any, is a new Flyway migration; existing migrations are never edited.
- [ ] The README and this document reflect the change.
- [ ] The build passes from a clean checkout.

## Release acceptance criteria

Stage 1 is done when all of these pass in CI against a real PostgreSQL started by Testcontainers.

- [ ] **AC-1 Concurrency:** 50 accounts, 10,000 random transfers across 32 threads; afterwards invariants I1 to I4 hold. The test passes ten runs in a row.
- [ ] **AC-2 Overspend:** 100 parallel withdrawals of ₹1 from an account holding ₹50; exactly 50 succeed, 50 are rejected, final balance is zero.
- [ ] **AC-3 Deadlock freedom:** 1,000 transfers A to B run in parallel with 1,000 transfers B to A; none fail with a deadlock error.
- [ ] **AC-4 Retry storm:** the same request sent 20 times in parallel with one idempotency key creates exactly one transaction, and all 20 responses are identical.
- [ ] **AC-5 Key misuse:** the same key with a different body returns 422 and changes nothing.
- [ ] **AC-6 Crash after commit:** a fault injected after commit and before the response, followed by a retry, yields one transaction and the original response.
- [ ] **AC-7 Rollback:** a failure injected at each step of a transfer leaves zero ledger entries and unchanged balances.
- [ ] **AC-8 Append-only:** an `UPDATE` or `DELETE` on `ledger_entries` is rejected by the database.
- [ ] **AC-9 Reconciliation:** after deliberately corrupting one balance, the reconciliation run reports exactly that account.
- [ ] **AC-10 Reproducibility:** `docker compose up` plus `./mvnw verify` runs everything from a clean checkout.

## Design

Ledgerline is one Spring Boot application in four layers, with PostgreSQL enforcing the rules that must never break even if the application has a bug.

**Package structure**

```text
com.ledgerline
  account/          Account, AccountRepository, AccountService, AccountController
  ledger/           Transaction, LedgerEntry, LedgerRepository, LedgerQueryController
  posting/          PostingService (deposit, withdraw, transfer), PostingController
  idempotency/      IdempotencyService, IdempotencyRepository, request hashing
  reconciliation/   ReconciliationService, ReconciliationJob, ReconciliationController
  common/           error codes, exception handler, Clock, configuration
```

**A transfer, step by step**

1. The controller validates the request body and requires an `Idempotency-Key` header.
2. `PostingService` opens one database transaction.
3. It inserts the idempotency key. On a unique-constraint conflict it reads the stored result and returns it.
4. It locks both accounts with `SELECT ... FOR UPDATE`, in ascending `id` order.
5. It checks status and currency, then debits the source with a conditional update (`balance_paise >= :x`).
6. It credits the destination, inserts the transaction and its two ledger entries, and stores the response against the key.
7. It commits. The database checks that the entries sum to zero at commit time.
8. The controller returns 201 with the transaction.

**Key decisions**

| Decision | Choice | Reason |
| --- | --- | --- |
| Money type | `BIGINT` paise | Exact arithmetic; floats cannot represent money |
| Source of truth | Ledger, with a balance column updated in the same transaction | Auditable history plus fast balance reads; reconciliation proves they agree |
| Overspend check | Conditional `UPDATE` after row locks | The database decides, so no read-then-write race |
| Deadlock avoidance | Lock accounts in ascending id order | Opposite transfers acquire locks in the same order |
| Isolation level | READ COMMITTED | Row locks provide the safety; tests must confirm it |
| Data access | Spring `JdbcClient` for the money path, JPA for simple reads | Explicit SQL where locking matters |
| Idempotency record | Stored in the same transaction as the ledger entries | Both commit or neither does |
| Rejected requests | Do not consume the idempotency key | A client may retry after fixing the cause, such as depositing first |
| Safety net | CHECK constraints and triggers in the database | A bug in the code cannot corrupt the ledger |
| Schema changes | Flyway migrations | Repeatable, versioned database setup |

## API contract

All money-moving endpoints require an `Idempotency-Key` header and amounts in whole paise. Every path is versioned under `/api/v1` (decided in Sprint 0, see `docs/decisions.md`).

| Method and path | Purpose | Success |
| --- | --- | --- |
| `POST /api/v1/accounts` | Create an account (`ownerName`, `type`) | 201 |
| `GET /api/v1/accounts/{publicId}` | Account and current balance | 200 |
| `GET /api/v1/accounts/{publicId}/ledger?before={entryId}&limit=50` | Ledger entries, newest first | 200 |
| `POST /api/v1/deposits` | `{accountId, amountPaise}` | 201 |
| `POST /api/v1/withdrawals` | `{accountId, amountPaise}` | 201 |
| `POST /api/v1/transfers` | `{fromAccountId, toAccountId, amountPaise}` | 201 |
| `GET /api/v1/transactions/{publicId}` | One transaction with its entries | 200 |
| `POST /api/v1/admin/reconciliation` | Run reconciliation now | 200 |
| `GET /api/v1/admin/reconciliation/latest` | Result of the latest run | 200 |

`publicId` values are server-generated UUIDs. The client never supplies one; it receives it in the
`201` response and uses it in every later call.

**Error codes**

| HTTP | Code | When |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | Missing header, non-positive amount, bad id |
| 404 | `ACCOUNT_NOT_FOUND` | Account does not exist |
| 409 | `REQUEST_IN_PROGRESS` | Same key is still being processed |
| 422 | `INSUFFICIENT_FUNDS` | Source balance is below the amount |
| 422 | `ACCOUNT_FROZEN` | An account in the request is frozen |
| 422 | `IDEMPOTENCY_KEY_REUSED` | Same key, different request body |
| 503 | `RETRYABLE_FAILURE` | Deadlock retries exhausted; safe to retry with the same key |

## Database schema

The database holds six tables and enforces the money rules itself: balances cannot go negative, ledger rows cannot be edited or deleted, and each transaction's entries must sum to zero.

The schema is built one table per sprint, in dependency order, so the exact DDL lives with the
code that uses it rather than in this document:

| Table | Added in | Migration |
| --- | --- | --- |
| `accounts` | Sprint 0 | `backend/src/main/resources/db/migration/V1__init.sql` |
| `transactions` (minimal parent) | Sprint 1 | `V2__...` |
| `ledger_entries` | Sprint 1 | `V2__...` |
| `idempotency_keys` | Sprint 5 | `V3__...` |
| `reconciliation_runs` | Sprint 7 | `V4__...` |
| `reconciliation_findings` | Sprint 7 | `V4__...` |

The ledger sits in the middle: every entry ties one account to one transaction, and idempotency keys and reconciliation results hang off those two.

### Invariants

These are the properties the whole project exists to protect. A change is not allowed to weaken
any of them, and Sprint 8 turns each one into an automated check.

| # | Invariant |
| --- | --- |
| I1 | The entries of a transaction sum to zero. Money is moved, never created or destroyed. |
| I2 | A customer or merchant account never holds a negative balance. Only `SYSTEM_FUNDING` may. |
| I3 | `ledger_entries` and `transactions` are append-only: `UPDATE` and `DELETE` are rejected by the database. |
| I4 | The sum of all account balances is always zero. |
| I5 | An account's stored balance equals the sum of that account's ledger entries. |
| I6 | Each `public_id` identifies exactly one account, and each account has exactly one `public_id`. |
| I7 | A transaction is posted all or nothing: either all of its entries are committed, or none are. |

**Why it is shaped this way**

- `ck_accounts_negative_balance` makes overspending impossible at the database level, even if the application check is wrong.
- The deferred trigger lets the code insert a transaction's entries one at a time; the zero-sum rule is checked once, at commit.
- The unique `(client_id, idempotency_key)` constraint is what makes parallel duplicate requests safe: exactly one insert wins.
- `idx_ledger_account_id_desc` serves the paged ledger query and the reconciliation sum per account.
- Two account columns, `id` for joins and locking and `public_id` for the API, keep internal numbering private.
- This schema is a first draft. Expect to adjust it when the first tests run against real PostgreSQL.

## Out of scope and roadmap

Stage 1 deliberately excludes everything that is not the financial core, so it can be finished and proven.

- **Out of scope for Stage 1:** real payment providers and real money, authentication and user management, multiple currencies, a React front end, Redis, multi-instance deployment.
- **Stage 2:** transactional outbox, background workers, webhook delivery with retries, backoff, leases and a dead-letter queue.
- **Stage 3:** rate limiting, multiple instances behind a load balancer, metrics and dashboards, CI and load tests with measured numbers.
- **Possible later extension:** an ATM-style transaction simulator built on top of the ledger. It stays a future idea until the ledger is complete and tested.
