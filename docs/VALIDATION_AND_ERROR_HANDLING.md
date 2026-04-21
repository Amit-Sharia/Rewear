# Validation and Error Handling (DBMS Phase 2)

## Objective
This document defines how validation and error handling are enforced in ReWear so data integrity does not depend only on UI checks.

## Validation Layers

### 1) Application-level validation
- Username: required, length bounded, allowed characters only.
- Email: required, format checked, max length bounded.
- Password: required, minimum length, mixed character classes.
- Password confirmation: exact match required.

### 2) Domain-level database validation
- Enum-style domains enforce legal states:
  - `user_role`
  - `item_condition`
  - `listing_status`
  - `swap_status`

### 3) Entity-level constraints
- Users:
  - `email` unique (case-insensitive where supported).
  - `points_balance >= 0`.
- Listings:
  - `status = DRAFT` implies `published_at IS NULL`.
- Swap requests:
  - `offered_points >= 0`.
  - uniqueness on duplicate offer combinations.
- Reviews:
  - `rating BETWEEN 1 AND 5`.
  - `reviewer_id <> reviewee_id`.

### 4) Relational integrity
- Foreign keys with explicit delete behavior:
  - `CASCADE` for strictly dependent rows.
  - `SET NULL` for history-preserving relations.

### 5) Trigger/procedure business validation
- Triggers validate business invariants before write.
- Trigger logic blocks illegal state mutations.
- Stored procedures centralize multi-step updates.

## Error Handling Strategy

### SQL/database-level
- Constraint and trigger failures raise SQL exceptions with clear messages.
- Transactional paths rollback on failure.

### Application mapping recommendation
- Constraint violations -> validation error response.
- Foreign-key violations -> conflict or bad request, based on use case.
- Trigger/business-rule violations -> unprocessable entity style response.

## Auditing and Observability
- Status/history tables and audit logs preserve important transitions.
- Optional per-session actor context can be attached for attribution.

## Safe Transaction Pattern (Recommended)
```sql
BEGIN;
-- perform related writes
COMMIT;
```

On any error:
```sql
ROLLBACK;
```

## Notes
- Use database constraints as the source of truth for data safety.
- Keep UI validation for fast user feedback, not as the only guardrail.
