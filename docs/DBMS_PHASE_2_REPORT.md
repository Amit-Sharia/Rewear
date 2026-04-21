# ReWear DBMS Phase 2 Report - Advanced MySQL Implementation

**Project**: ReWear - Community Clothing Exchange  
**Author**: Amit  
**Date**: Updated 2026  
**Database**: MySQL 8.0+  

## Executive Summary

Phase 2 extends the Phase 1 relational schema with advanced DBMS objects and aligns the Java DAO/UI workflow with those objects.

| Feature | Status | Implementation |
|---------|--------|----------------|
| Functions | Implemented | `fn_user_available_items`, `fn_owner_average_rating` |
| Stored Procedure | Implemented | `sp_complete_exchange` (transactional completion) |
| Triggers | Implemented | rating validation, negative balance prevention, balance auto-apply |
| DAO Integration | Implemented | completion now calls procedure; points updates rely on `POINTS_LOG` triggers |
| UI Integration | Implemented | dashboard stats from functions; requests moved to Accept -> Complete flow |
| Demo Data | Implemented | idempotent sample inserts for repeatable reruns |

---

## 1. Phase 2 DB Objects

### 1.1 Functions

- `fn_user_available_items(p_user_id)`  
  Returns count of `ITEMS` owned by user where `status = 'AVAILABLE'`.
- `fn_owner_average_rating(p_owner_user_id)`  
  Returns average rating for exchanges where the user is the owner (`0.00` when none).

### 1.2 Stored Procedure

- `sp_complete_exchange(p_exchange_id)` performs:
  1. Lock target exchange (`FOR UPDATE`)
  2. Validate exchange exists and is `ACCEPTED`
  3. Compute total points from `EXCHANGE_ITEMS`
  4. Insert debit/credit rows in `POINTS_LOG`
  5. Mark involved items as `EXCHANGED`
  6. Mark exchange as `COMPLETED`
  7. Commit transaction

### 1.3 Triggers

| Trigger | Timing | Table | Purpose |
|---------|--------|-------|---------|
| `trg_reviews_rating_check` | BEFORE INSERT | `REVIEWS` | Reject ratings outside 1-5 |
| `trg_points_log_prevent_negative` | BEFORE INSERT | `POINTS_LOG` | Block inserts causing negative balance |
| `trg_points_log_apply_balance` | AFTER INSERT | `POINTS_LOG` | Apply `delta` to `USERS.points_balance` |

---

## 2. Schema and Relationships

Core operational entities:
- `USERS`
- `ITEMS`
- `EXCHANGES`
- `EXCHANGE_ITEMS`
- `POINTS_LOG`
- `REVIEWS`
- `EXCHANGE_MESSAGES`
- `SHIPPING`
- `ITEM_IMAGES`

Lookup (normalization support):
- `CATEGORY`
- `SIZE`
- `ITEM_CONDITION`

High-level data flow:
1. User owns items (`USERS` -> `ITEMS`)
2. Request creates exchange (`EXCHANGES`) and selected items (`EXCHANGE_ITEMS`)
3. Completion logs points (`POINTS_LOG`) and updates balances via trigger
4. Completion enables review/chat history (`REVIEWS`, `EXCHANGE_MESSAGES`)

---

## 3. Java Integration (Updated)

Phase 2 is now integrated into DAO and UI behavior.

### 3.1 DAO updates

- `ExchangeDAO.acceptRequest(...)` now sets status to `ACCEPTED` only.
- `ExchangeDAO.completeAcceptedRequest(...)` calls `CALL sp_complete_exchange(?)`.
- `UserDAO.addPoints(...)` now writes to `POINTS_LOG`; balance is updated by trigger.
- `UserDAO` includes function reads:
  - `getAvailableItemsCount(...)`
  - `getOwnerAverageRating(...)`

### 3.2 UI updates

- `ManageRequestsFrame`: split actions into `Accept` and `Complete`.
- `DashboardFrame`: shows
  - available items count (function-driven)
  - average owner rating (function-driven)
- Chat eligibility adjusted to support accepted/completed lifecycle.

---

## 4. Repeatable Deployment

Schema reruns are now safer:
- advanced objects dropped with `DROP ... IF EXISTS`
- sample exchange and exchange-item inserts guarded with `NOT EXISTS`
- sample users use `ON DUPLICATE KEY UPDATE` to avoid duplicate key failures

Execution command used:
```bash
"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p***** -e "source C:/Users/Amit/Desktop/Reware_Final/src/main/java/com/rewear/models/sql/schema.sql"
```

---

## 5. Verification

Verified after deployment:
- function call works (`fn_user_available_items(...)`)
- procedure exists (`sp_complete_exchange`)
- all three triggers are present and active

Recommended smoke test:
```sql
USE rewear;
SELECT fn_user_available_items((SELECT user_id FROM USERS WHERE username='alice' LIMIT 1));
UPDATE EXCHANGES SET status='ACCEPTED' WHERE exchange_id=1;
CALL sp_complete_exchange(1);
SELECT * FROM POINTS_LOG ORDER BY created_at DESC LIMIT 5;
```

---

## 6. Conclusion

Phase 2 goals are achieved with full DB + app alignment:
- advanced DB objects implemented
- transactional consistency moved into stored procedure
- points integrity enforced centrally by triggers
- UI now surfaces function-based DB insights
- schema can be rerun safely for demo/viva preparation
