# ReWear Viva Guide - Database Focus

## 1. What problem the database solves

ReWear is a clothing exchange platform where users list items, request exchanges, transfer points, and leave reviews.  
The database must guarantee:
- consistent balances (no negative points)
- valid reviews (rating 1 to 5)
- traceable exchange lifecycle (`PENDING` -> `ACCEPTED` -> `COMPLETED`)
- no duplicate or inconsistent lookup values

---

## 2. Previous schema issues (before normalization + phase 2)

In early design, common risks were:
- repeated textual attributes in `ITEMS` (category/size/condition as plain text)
- business rules only in UI/DAO (risk of bypass by direct SQL)
- manual multi-query exchange completion (partial update anomalies if one step failed)
- point updates and logs handled separately (risk of mismatch)

These are classic anomaly risks:
- **Update anomaly**: changing a category name in many rows
- **Insert anomaly**: cannot insert a new category unless an item exists
- **Delete anomaly**: deleting last item may lose category information

---

## 3. How normalization was applied

## 3.1 1NF
- Atomic fields only; no repeating groups.
- Separate `EXCHANGE_ITEMS` table for many-to-many relation between `EXCHANGES` and `ITEMS`.

## 3.2 2NF
- Non-key attributes depend on full key.
- Junction table attributes tied to exchange-item relation.

## 3.3 3NF
- Lookup data split into dedicated tables:
  - `CATEGORY`
  - `SIZE`
  - `ITEM_CONDITION`
- `ITEMS` stores only foreign keys to lookup values.
- Derived processes (points transfer, rating rule) implemented as DB logic, not duplicated columns.

Result: reduced redundancy, fewer inconsistencies, cleaner updates.

---

## 4. Final schema map (what table is where)

### Core actors and inventory
- `USERS`: account and points balance
- `ITEMS`: listed products linked to owner and lookup tables
- `ITEM_IMAGES`: one-to-many images for each item

### Exchange transaction flow
- `EXCHANGES`: request between requester and owner
- `EXCHANGE_ITEMS`: which item(s) are part of an exchange
- `SHIPPING`: shipping/pickup details
- `EXCHANGE_MESSAGES`: chat between parties

### Trust and accounting
- `REVIEWS`: post-completion feedback
- `POINTS_LOG`: immutable points ledger (`delta` entries)

### Lookup / master data
- `CATEGORY`, `SIZE`, `ITEM_CONDITION`

---

## 5. Advanced DBMS objects and why they matter

### Functions
- `fn_user_available_items(user_id)`
  - dashboard metric for available inventory
- `fn_owner_average_rating(owner_user_id)`
  - reputation metric for the owner

### Stored procedure
- `sp_complete_exchange(exchange_id)`
  - centralizes completion logic in one transaction
  - prevents partial completion states
  - ensures point logs, item status, and exchange status stay synchronized

### Triggers
- `trg_reviews_rating_check`
  - blocks invalid ratings
- `trg_points_log_prevent_negative`
  - prevents balance from dropping below zero
- `trg_points_log_apply_balance`
  - automatically applies ledger changes to `USERS.points_balance`

Design principle: **POINTS_LOG is source-of-truth ledger; balance is maintained by trigger**.

---

## 6. How operations work end-to-end

## 6.1 Request exchange
1. requester selects available item
2. create row in `EXCHANGES` with `PENDING`
3. add selected item in `EXCHANGE_ITEMS`

## 6.2 Owner accepts
1. owner validates pending request
2. status moves to `ACCEPTED` (no points moved yet)

## 6.3 Owner completes
1. app calls `CALL sp_complete_exchange(exchange_id)`
2. procedure inserts debit/credit in `POINTS_LOG`
3. trigger checks negative balance before insert
4. trigger updates `USERS.points_balance` after insert
5. procedure marks item `EXCHANGED` and exchange `COMPLETED`

## 6.4 Review
1. completed exchange eligible for review
2. insert into `REVIEWS`
3. trigger validates rating range 1-5

---

## 7. Key viva talking points

- Why normalize?
  - reduce redundancy and anomalies
  - improve consistency and maintainability
- Why triggers?
  - enforce integrity even if UI is bypassed
- Why procedure?
  - ACID transaction for multi-table state transition
- Why points ledger?
  - auditable financial-like trail (`POINTS_LOG`)
  - easier debugging and rollback reasoning

---

## 8. Demo script for viva

```sql
USE rewear;

-- A) show function outputs
SELECT fn_user_available_items((SELECT user_id FROM USERS WHERE username='alice' LIMIT 1)) AS alice_available_items;
SELECT fn_owner_average_rating((SELECT user_id FROM USERS WHERE username='alice' LIMIT 1)) AS alice_avg_rating;

-- B) show lifecycle transition
SELECT exchange_id, status FROM EXCHANGES ORDER BY exchange_id DESC LIMIT 3;
UPDATE EXCHANGES SET status='ACCEPTED' WHERE exchange_id=1;
CALL sp_complete_exchange(1);
SELECT exchange_id, status FROM EXCHANGES WHERE exchange_id=1;

-- C) show ledger + balance effect
SELECT user_id, points_balance FROM USERS WHERE username IN ('alice','bob');
SELECT user_id, delta, reason, related_exchange_id, created_at
FROM POINTS_LOG
ORDER BY log_id DESC
LIMIT 5;
```

---

## 9. One-minute viva summary

ReWear moved from only table-level design to rule-driven relational design.  
Normalization reduced redundancy via lookup tables and junction tables.  
Phase 2 pushed critical invariants into MySQL itself through functions, triggers, and a transactional stored procedure.  
This guarantees data integrity for points, reviews, and exchange lifecycle regardless of UI behavior.
