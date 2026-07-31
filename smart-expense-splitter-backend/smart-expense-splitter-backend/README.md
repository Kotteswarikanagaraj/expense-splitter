# Smart Expense Splitter — Backend (Phase 1)

A Splitwise-style group expense tracker. Phase 1 covers: JWT auth, Users, Groups,
Group membership, and Expenses with automatic EQUAL/EXACT/PERCENTAGE splitting.
WebSocket (live updates) comes in a later phase.

## Tech Stack
- Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- MySQL 8
- JWT (jjwt library)
- Lombok

## Running locally

1. Create the database (or let it auto-create — see `application.properties`):
   ```sql
   CREATE DATABASE expense_splitter;
   ```
2. Update `src/main/resources/application.properties` with your MySQL username/password.
3. `mvn spring-boot:run`
4. API available at `http://localhost:8080`

## API Summary

| Method | Endpoint                          | Auth? | Description                          |
|--------|------------------------------------|-------|---------------------------------------|
| POST   | /api/auth/register                 | No    | Create account, returns JWT           |
| POST   | /api/auth/login                    | No    | Login, returns JWT                    |
| GET    | /api/users/me                      | Yes   | Current logged-in user's profile      |
| GET    | /api/users/{id}                    | Yes   | Any user's public profile             |
| POST   | /api/groups                        | Yes   | Create a group (creator auto-joins)   |
| POST   | /api/groups/{groupId}/members      | Yes   | Add a member (must be existing member)|
| GET    | /api/groups/my                     | Yes   | List groups you belong to             |
| GET    | /api/groups/{groupId}              | Yes   | Group details + member list           |
| POST   | /api/expenses                      | Yes   | Add expense, auto-creates shares      |
| GET    | /api/expenses/group/{groupId}      | Yes   | All expenses for a group              |

All authenticated endpoints require header: `Authorization: Bearer <token>`.

### Example: add an equal-split expense
```json
POST /api/expenses
{
  "groupId": 1,
  "description": "Dinner",
  "amount": 900.00,
  "paidBy": 2,
  "splitType": "EQUAL"
}
```

### Example: add an exact-split expense
```json
POST /api/expenses
{
  "groupId": 1,
  "description": "Groceries",
  "amount": 500.00,
  "paidBy": 2,
  "splitType": "EXACT",
  "shares": [
    { "userId": 2, "value": 300.00 },
    { "userId": 3, "value": 200.00 }
  ]
}
```

## Database Schema (ER description)

**Entities and relationships:**

- **User** (`users`) — a registered account. `id`, `name`, `email` (unique), `password` (BCrypt hash), `createdAt`.

- **Group** (`expense_groups`) — a shared expense circle (e.g. "Goa Trip").
  `id`, `name`, `created_by` → FK to `users.id`, `createdAt`.
  - One `User` can create **many** `Group`s → `User (1) --- (N) Group`

- **GroupMember** (`group_members`) — resolves the **many-to-many** between
  `User` and `Group` (a user can be in many groups, a group has many users),
  plus carries the extra `joinedAt` attribute that a plain join table couldn't hold.
  Composite primary key: `(group_id, user_id)`.
  - `Group (1) --- (N) GroupMember (N) --- (1) User`

- **Expense** (`expenses`) — one bill paid within a group.
  `id`, `group_id` → FK to `expense_groups.id`, `description`, `amount`, `paid_by` → FK
  to `users.id`, `split_type` (EQUAL/EXACT/PERCENTAGE), `createdAt`.
  - `Group (1) --- (N) Expense`
  - `User (1) --- (N) Expense` (as payer, via `paid_by`)

- **ExpenseShare** (`expense_shares`) — for one `Expense`, how much each group
  member owes. This is what gets generated automatically when an expense is created.
  `id`, `expense_id` → FK to `expenses.id`, `user_id` → FK to `users.id`, `share_amount`.
  - `Expense (1) --- (N) ExpenseShare (N) --- (1) User`
  - Example: a ₹900 dinner split equally 3 ways produces 3 `ExpenseShare` rows of ₹300 each.

- **Settlement** (`settlements`) — records a payment made between two users to
  settle a debt within a group (not yet wired to an endpoint in Phase 1; the
  table exists so the schema is complete ahead of the "settle up" feature).
  `id`, `group_id`, `from_user`, `to_user`, `amount`, `settledAt`.

**Simplified ER diagram:**
```
User ──< Group (created_by)
User ──< GroupMember >── Group        (resolves the M:N membership)
Group ──< Expense
User  ──< Expense (paid_by)
Expense ──< ExpenseShare >── User     (who owes what, per expense)
Group ──< Settlement >── User (from_user, to_user)
```

## Key Design Decisions (for interview defense)

1. **DTOs everywhere, entities never leave the service layer.** Prevents leaking
   sensitive fields (e.g. password hash) and decouples the API contract from the
   DB schema.

2. **GroupMember as an explicit join entity, not `@ManyToMany`.** A plain
   `@ManyToMany` auto-generates a join table with no room for `joinedAt`. Using
   `@EmbeddedId` + `@MapsId` gives a real entity with a composite key that reuses
   the FK columns instead of duplicating them.

3. **BigDecimal for all money fields**, never `double`/`float` — avoids binary
   floating-point rounding errors, which matters a lot once you're summing shares
   that must reconcile to a total.

4. **Stateless JWT auth** (`SessionCreationPolicy.STATELESS`) — no server-side
   session, so the API scales horizontally; every request is self-contained via
   the Bearer token.

5. **Authorization (group membership checks) lives in the service layer**, not
   annotations — because "is this user a member of this group" is data-dependent
   and needs a DB query, unlike role-based checks that Spring Security's
   `@PreAuthorize` handles well.

6. **Centralized exception handling via `@RestControllerAdvice`** — every error
   response (validation, not-found, forbidden, auth failure, unexpected 500) has
   one consistent JSON shape (`ApiError`), and the generic `Exception` handler
   ensures no internal stack trace ever leaks to the client.

7. **Split calculation as a strategy-per-enum-value (`switch` on `SplitType`)** —
   EQUAL, EXACT, and PERCENTAGE each have their own validation + calculation
   method, called from one `addExpense()` entry point. Easy to extend later
   (e.g. adding a SHARES/weighted split type).

## What's NOT in Phase 1 (intentionally deferred)
- WebSocket / STOMP live updates
- Settlement ("settle up") endpoint and balance calculation
- Role-based access control (admin vs member)
- Refresh tokens (current JWT is access-token-only, 24h expiry)

---

# Phase 2 — Debt Simplification, Settlements, Pagination

## New/changed endpoints

| Method | Endpoint                                             | Description                                         |
|--------|-------------------------------------------------------|-------------------------------------------------------|
| GET    | /api/groups/{groupId}/settlements                     | Recomputes balances, returns minimal payment list     |
| POST   | /api/groups/{groupId}/settlements/{settlementId}/settle | Marks a suggested settlement as actually paid        |
| GET    | /api/expenses/group/{groupId}?page=&size=&sortBy=&sortDir=&paidBy=&splitType=&minAmount=&maxAmount=&from=&to=&description= | Paginated + filtered expense list (all params optional) |

## The debt-simplification algorithm

**Problem:** after N expenses in a group, don't ask everyone to settle every
individual expense-share pairwise — that's a mess. Compute one minimal set of
payments that clears every debt.

**Step 1 — net balance per person:**
```
balance(user) = total the user PAID (as an expense's paidBy)
              - total the user OWES (sum of their ExpenseShare rows)
              + amounts they've already paid via settled Settlements
              - amounts they've already received via settled Settlements
```
Positive = creditor (owed money). Negative = debtor (owes money). These always
sum to zero across the group — every rupee overpaid by someone is a rupee
underpaid by someone else (double-entry accounting).

**Step 2 — greedy two-pointer matching:**
1. Sort debtors by debt size (largest first), creditors by credit size (largest first).
2. Match the biggest debtor with the biggest creditor. Payment = `min(their two amounts)`.
3. Whichever side hits zero drops out; the other's leftover carries to the next match.
4. Repeat until both lists are exhausted.

**Why greedy works:** every payment fully zeroes out at least one person (we
always pay the smaller of the two amounts), so with N people we need **at most
N-1 payments**. The very last person left must land on exactly zero because the
whole group's balances always sum to zero.

**Complexity:** O(N log N) — dominated by sorting; the matching sweep itself is
a single O(N) pass with two pointers.

**Honest caveat:** this greedy result is *at most* N-1 transactions and matches
what real apps like Splitwise do — but proving the absolute global minimum in
every edge case is an NP-hard partition-style problem. Greedy is the accepted
practical tradeoff: fast, simple, near-optimal.

## Settlement lifecycle

`Settlement` rows have two states:
- **Pending** (`settled=false`): a suggestion generated by the algorithm. Every
  call to `GET /settlements` deletes the group's old pending rows and inserts a
  freshly computed batch — they're fully derived, not source-of-truth, so
  there's no harm recomputing them.
- **Settled** (`settled=true`, `settledAt` populated): a real payment someone
  confirmed via `POST /settle`. These are never deleted — they're historical
  record, and they're what future balance calculations subtract out.

## Pagination & filtering design

- Uses Spring Data's `Pageable` (`PageRequest.of(page, size, sort)`), built in
  the controller from `page`, `size`, `sortBy`, `sortDir` query params.
- Filtering uses **JPA Specifications** (`ExpenseSpecifications`) instead of
  one repository method per filter combination. Each filter is a small lambda
  building one Criteria API predicate; `Specification.where(null)` and
  `.and(null)` are no-ops, so any filter the caller didn't supply simply
  contributes nothing to the WHERE clause — no `if/else` branching needed to
  assemble the query.
- Response is wrapped in a custom `PageResponse<T>` DTO (not Spring's raw
  `Page<T>`) so the JSON shape only exposes what a frontend needs
  (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`) instead of
  Spring's internal `Pageable`/`Sort` metadata.

## Key Design Decisions Added in Phase 2 (for interview defense)

8. **Pending settlements are recomputed, not incrementally updated.** Simpler
   and always-correct: rather than trying to patch existing suggestions when a
   new expense is added, we just wipe and regenerate from the current source
   of truth (expenses + settled settlements) on every read.

9. **Two-pointer greedy over a max-heap.** Both are O(N log N), but sorting two
   plain lists once up front is simpler to implement and explain than
   maintaining two priority queues — a good example of picking the simpler
   correct solution when both have the same asymptotic complexity.

10. **Specification pattern over multiple repository methods** for filtering —
    avoids a combinatorial explosion of query methods (`findByGroupAndPaidBy`,
    `findByGroupAndSplitType`, `findByGroupAndPaidByAndSplitType`, ...) as more
    filters get added.

---

# Phase 3 — WebSocket Live Updates

## New endpoint
`/ws` — STOMP-over-SockJS handshake endpoint (not a REST endpoint; the frontend
connects here with `@stomp/stompjs` + `sockjs-client`).

## How it works
1. `WebSocketConfig` registers `/ws` as the connection endpoint and enables a
   simple in-memory STOMP broker on the `/topic` prefix.
2. `GroupNotificationService` wraps `SimpMessagingTemplate` — the one class
   responsible for pushing messages. `ExpenseService.addExpense()` and
   `SettlementService.markAsSettled()` call it right after their save succeeds.
3. Every message goes to `/topic/group/{groupId}` as a `GroupEventMessage`
   payload: `{ type, groupId, data, timestamp }`. `type` is either
   `EXPENSE_ADDED` or `SETTLEMENT_UPDATED`.
4. Any client subscribed to that exact topic (i.e. anyone currently viewing
   that group in the frontend) receives the message and refetches.

## Design notes worth defending
- **Simple broker, not RabbitMQ.** Spring's built-in in-memory broker is fine
  for a single app instance (a resume project). A real multi-instance
  production deployment needs an external broker (e.g. RabbitMQ with STOMP
  support) so a message published on instance A reaches a client connected to
  instance B.
- **Sockets are for notification, not data.** The WS payload carries the
  changed DTO as a hint, but the frontend re-fetches from REST anyway (via
  the `refreshSignal` pattern) rather than trusting the socket payload as the
  full source of truth. This keeps exactly one source of truth (the REST API)
  and avoids the socket and REST data models silently drifting apart.
- **No message-level auth in Phase 3.** `/ws/**` is `permitAll()` in
  `SecurityConfig`, and there's no check that a subscriber is actually a
  member of the group they're subscribing to. For a resume project this is a
  reasonable, explicitly-flagged simplification — the natural next step would
  be a `ChannelInterceptor` on the STOMP `CONNECT`/`SUBSCRIBE` frames that
  validates the JWT and checks group membership before allowing the
  subscription.
- **Broadcast timing.** The broadcast call happens inside the same
  `@Transactional` service method as the save, before the transaction
  actually commits at the proxy boundary. The fully-correct version would
  defer the broadcast to `TransactionSynchronizationManager`'s `afterCommit`
  hook so a client never reacts to a change that could still roll back. Worth
  mentioning proactively if asked "what would you improve."
