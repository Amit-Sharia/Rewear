# ReWear Project - DBMS Phase 1 Report
## Database Design & Analysis

**Project:** ReWear - Community Clothing Exchange Platform  
**Team Member:** Amit  
**Date:** April 3, 2026  
**Database:** MySQL 8.0+  
**Character Set:** UTF-8MB4

---

## Executive Summary

This report documents the database design phase for the ReWear desktop application, a Java Swing platform enabling community members to exchange clothing items. The database supports user management, item cataloging, exchange transactions, messaging, and review systems.

**Database Schema:** 11 tables with normalized structure (3NF)  
**Total Relationships:** 15 (1:M, 1:1)  
**Primary Focus:** Data integrity, user transactions, and scalability

---

## 1. Requirements Analysis

### 1.1 Functional Requirements
- **User Management**: Registration, login, profile maintenance, points tracking
- **Item Management**: Add/browse/delete items with categories and conditions
- **Exchange System**: Request exchanges, manage status, complete transactions
- **Messaging**: Communication between exchange participants
- **Point System**: Award/deduct points for transactions
- **Reviews**: Rate and comment on completed exchanges

### 1.2 Data Requirements
- Store user credentials and account balance
- Maintain item inventory with metadata (category, size, condition)
- Track exchange history with status workflow
- Enable messaging within exchange context
- Log point transactions for audit trail

---

## 2. Database Entities & Attributes

### 2.1 Core Entity Tables

#### USERS Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| user_id | INT | PK, AUTO_INCREMENT | Unique user identifier |
| username | VARCHAR(64) | UNIQUE, NOT NULL | Login identifier |
| email | VARCHAR(255) | NOT NULL | Contact & recovery |
| password_hash | VARCHAR(255) | NOT NULL | Authentication (demo: plaintext) |
| points_balance | INT | DEFAULT 100 | Exchange currency |
| created_at | TIMESTAMP | DEFAULT NOW() | Account creation timestamp |

**Index:** `idx_users_username` for fast login lookups

**Purpose:** Central user registry with points-based economy

---

#### ITEMS Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| item_id | INT | PK, AUTO_INCREMENT | Unique item identifier |
| owner_user_id | INT | FK (USERS), NOT NULL | Item owner |
| category_id | INT | FK (CATEGORY), NOT NULL | Product classification |
| size_id | INT | FK (SIZE), NOT NULL | Clothing size |
| condition_id | INT | FK (ITEM_CONDITION), NOT NULL | Item quality rating |
| item_name | VARCHAR(255) | NOT NULL | Product name |
| brand | VARCHAR(128) | DEFAULT '' | Manufacturer |
| description | TEXT | Optional | Detailed information |
| points_value | INT | DEFAULT 0 | Exchange cost |
| status | VARCHAR(32) | DEFAULT 'AVAILABLE' | AVAILABLE, EXCHANGED, REMOVED |
| created_at | TIMESTAMP | DEFAULT NOW() | Listing date |

**Purpose:** Inventory of available items for exchange

---

#### EXCHANGES Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| exchange_id | INT | PK, AUTO_INCREMENT | Unique exchange identifier |
| requester_user_id | INT | FK (USERS), NOT NULL | User requesting item |
| owner_user_id | INT | FK (USERS), NOT NULL | Item owner |
| status | VARCHAR(32) | DEFAULT 'PENDING' | Workflow state |
| created_at | TIMESTAMP | DEFAULT NOW() | Request timestamp |

**Status Workflow:** PENDING → ACCEPTED → COMPLETED or REJECTED

**Purpose:** Transaction tracking between users

---

### 2.2 Relationship Tables

#### EXCHANGE_ITEMS Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| exchange_item_id | INT | PK, AUTO_INCREMENT | Composite key alternative |
| exchange_id | INT | FK (EXCHANGES), NOT NULL | Parent exchange |
| item_id | INT | FK (ITEMS), NOT NULL | Exchanged item |

**Purpose:** Maps items to exchanges (many items per exchange possible)

---

#### EXCHANGE_MESSAGES Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| message_id | INT | PK, AUTO_INCREMENT | Unique message ID |
| exchange_id | INT | FK (EXCHANGES), NOT NULL | Parent exchange |
| sender_user_id | INT | FK (USERS), NOT NULL | Message author |
| message_text | TEXT | NOT NULL | Message content |
| sent_at | TIMESTAMP | DEFAULT NOW() | Send time |

**Purpose:** In-exchange communication thread

---

#### SHIPPING Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| shipping_id | INT | PK, AUTO_INCREMENT | Unique shipment ID |
| exchange_id | INT | FK (EXCHANGES), UNIQUE | 1:1 with exchange |
| address_line | VARCHAR(512) | NOT NULL | Delivery address |
| status | VARCHAR(64) | DEFAULT 'PENDING' | PENDING, IN_TRANSIT, DELIVERED |

**Purpose:** Logistics tracking per exchange

---

### 2.3 Lookup Tables

#### CATEGORY Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| category_id | INT | PK, AUTO_INCREMENT | Category ID |
| name | VARCHAR(128) | UNIQUE, NOT NULL | Category name (Tops, Dresses, etc.) |

**Purpose:** Item classification

---

#### SIZE Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| size_id | INT | PK, AUTO_INCREMENT | Size ID |
| label | VARCHAR(64) | UNIQUE, NOT NULL | Size label (XS, S, M, L, XL, etc.) |

**Purpose:** Standardized sizing

---

#### ITEM_CONDITION Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| condition_id | INT | PK, AUTO_INCREMENT | Condition ID |
| label | VARCHAR(64) | UNIQUE, NOT NULL | Condition (Like New, Good, Fair, Poor) |

**Purpose:** Item quality rating

---

### 2.4 Audit Tables

#### POINTS_LOG Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| log_id | INT | PK, AUTO_INCREMENT | Log entry ID |
| user_id | INT | FK (USERS), NOT NULL | User affected |
| delta | INT | NOT NULL | Points added/subtracted (±) |
| reason | VARCHAR(255) | NOT NULL | Transaction reason |
| related_exchange_id | INT | Optional | Associated exchange |
| created_at | TIMESTAMP | DEFAULT NOW() | Timestamp |

**Purpose:** Audit trail for point transactions

---

#### REVIEWS Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| review_id | INT | PK, AUTO_INCREMENT | Review ID |
| exchange_id | INT | FK (EXCHANGES), NOT NULL | Reviewed exchange |
| reviewer_user_id | INT | FK (USERS), NOT NULL | Author of review |
| rating | INT | NOT NULL | 1-5 stars |
| comment | TEXT | Optional | Review text |
| created_at | TIMESTAMP | DEFAULT NOW() | Review date |

**Purpose:** Feedback system for quality assurance

---

#### ITEM_IMAGES Table
| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| image_id | INT | PK, AUTO_INCREMENT | Image ID |
| item_id | INT | FK (ITEMS, CASCADE), NOT NULL | Parent item |
| image_url | VARCHAR(1024) | NOT NULL | Image URL/path |

**Purpose:** Multi-image gallery per item

---

## 3. Normalization Analysis

### 3.1 First Normal Form (1NF) ✅
**Criteria:** All attributes are atomic; no repeating groups

**Evidence:**
- All columns contain single, indivisible values
- No arrays, lists, or JSON objects in fields
- Example: `item_name` is a single string, not a list

---

### 3.2 Second Normal Form (2NF) ✅
**Criteria:** All non-key attributes are fully dependent on the entire primary key (not partial)

**Evidence:**
- All tables have surrogate primary keys (single INT column)
- All non-key attributes depend on the complete PK
- Example in ITEMS: `brand` depends on `item_id`, not partial on it

**Partial Dependency Check:** NONE - No composite PKs with problematic dependencies

---

### 3.3 Third Normal Form (3NF) ✅
**Criteria:** No transitive dependencies between non-primary-key attributes

**Evidence:**
- Non-key attributes do not depend on other non-key attributes
- Lookup tables (CATEGORY, SIZE, ITEM_CONDITION) segregated for independence
- Example: `item.brand` does not depend on `item.category_id`

**Transitive Dependency Check:**
- ✅ ITEMS.category_id → CATEGORY.name (normalized via FK)
- ✅ ITEMS.size_id → SIZE.label (normalized via FK)
- ✅ ITEMS.condition_id → ITEM_CONDITION.label (normalized via FK)

---

### 3.4 Boyce-Codd Normal Form (BCNF)
**Criteria:** For every functional dependency, the determinant is a candidate key

**Analysis:** All determinants (PKs, UNIQUEs) are candidate keys ✅

**Edge Case Review:** No anomalies detected

---

## 4. Referential Integrity Design

### 4.1 Foreign Key Relationships

```
USERS (1) ──────M──── ITEMS (ownership)
USERS (1) ──────M──── EXCHANGES (requester)
USERS (1) ──────M──── EXCHANGES (owner)
USERS (1) ──────M──── EXCHANGE_MESSAGES (sender)
USERS (1) ──────M──── REVIEWS (reviewer)
USERS (1) ──────M──── POINTS_LOG

ITEMS (1) ──────M──── EXCHANGE_ITEMS
EXCHANGES (1) ──────M──── EXCHANGE_ITEMS
CATEGORY (1) ──────M──── ITEMS
SIZE (1) ──────M──── ITEMS
ITEM_CONDITION (1) ──────M──── ITEMS
ITEMS (1) ──────M──── ITEM_IMAGES (CASCADE DELETE)
EXCHANGES (1) ──────M──── EXCHANGE_MESSAGES (CASCADE DELETE)
EXCHANGES (1) ──────M──── REVIEWS
EXCHANGES (1) ──────1──── SHIPPING
```

### 4.2 Cascade Delete Strategy

**Enabled for:**
- `ITEM_IMAGES` on `ITEMS` deletion
- `EXCHANGE_MESSAGES` on `EXCHANGES` deletion
- `EXCHANGE_ITEMS` on `EXCHANGES` deletion
- `SHIPPING` on `EXCHANGES` deletion

**Rationale:** Dependent records are meaningless when parent is deleted

---

## 5. Performance Considerations

### 5.1 Indexes

**Defined Indexes:**
- `idx_users_username`: Used in login queries (high frequency)

**Implicit Indexes:**
- PKs and FKs automatically indexed in MySQL

**Query Optimization:**
- JOIN on ITEMS → CATEGORY/SIZE/ITEM_CONDITION: Fast via FK indexes
- SELECT for available items: Filtered by status = 'AVAILABLE'

### 5.2 Scalability

**Design scales to:**
- 1M+ users (INT supports 2.1B)
- 10M+ items (with proper archival)
- 100M+ exchanges (with partitioning if needed later)

**Recommendations for growth:**
1. Partition POINT_LOG and REVIEWS by date
2. Archive old EXCHANGES after 1 year
3. Add caching for CATEGORY, SIZE, ITEM_CONDITION lookups

---

## 6. Security Considerations

### 6.1 Current Design
- ✅ Parameterized queries used (PreparedStatement in Java)
- ✅ Unique constraint on username (no duplicates)
- ⚠️ Password stored in plaintext (demo only - should use bcrypt/SHA-256)
- ✅ PII separation (email in USERS, no SSN/payment)

### 6.2 Recommendations
1. Implement proper password hashing (bcrypt, Argon2, or scrypt)
2. Add role-based access control (admin, moderator, user)
3. Implement audit logging with user actions
4. Add encryption for sensitive data in transit

---

## 7. Data Dictionary Summary

| Table | Rows | Purpose | Access Pattern |
|-------|------|---------|-----------------|
| USERS | 100-1000 | User accounts | Random (by username/ID) |
| ITEMS | 1000-10000 | Product inventory | Sequential (browse), Random (byID) |
| EXCHANGES | 500-5000 | Transactions | By user, by status |
| EXCHANGE_ITEMS | 500-10000 | Exchange details | By exchange_id |
| EXCHANGE_MESSAGES | 1000-50000 | Communication | By exchange_id, ordered by time |
| SHIPPING | 500-5000 | Logistics | By exchange_id |
| REVIEWS | 100-2000 | Feedback | By exchange_id, by reviewer |
| POINTS_LOG | 500-5000 | Audit | By user_id, ordered by time |
| ITEM_IMAGES | 2000-20000 | Gallery | By item_id |
| CATEGORY | 10-20 | Lookup | All (cached) |
| SIZE | 10-15 | Lookup | All (cached) |
| ITEM_CONDITION | 4-5 | Lookup | All (cached) |

---

## 8. Conclusion

The ReWear database schema successfully implements:

✅ **3rd Normal Form** with no anomalies  
✅ **Referential Integrity** with appropriate cascading  
✅ **Security Constraints** (UNIQUE, PKs, FKs)  
✅ **Scalability** for anticipated growth  
✅ **Auditability** with POINTS_LOG and REVIEWS  

The design supports all functional requirements while maintaining data consistency and enabling efficient queries through strategic indexing.

---

## Appendix A: Sample Data Distribution

### Expected data volumes by month:
- Week 1: 50 users, 200 items, 25 exchanges
- Month 1: 500 users, 2000 items, 300 exchanges
- Year 1: 10000 users, 50000 items, 10000 exchanges

### Most frequent queries:
1. `SELECT` available items by category
2. `SELECT` user's items
3. `SELECT` exchange status
4. `UPDATE` points on transaction
5. `INSERT` new exchange request

