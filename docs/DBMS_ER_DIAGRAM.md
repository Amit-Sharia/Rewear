# ReWear Database - Entity Relationship Diagram

## ER Diagram (ASCII + Markdown)

```
┌─────────────────────┐           ┌──────────────────────┐
│      USERS          │           │     CATEGORY         │
├─────────────────────┤           ├──────────────────────┤
│ PK: user_id         │           │ PK: category_id      │
│    username (U)     │◄──────────┤    name (U)          │
│    email            │  1:M      │                      │
│    password_hash    │           └──────────────────────┘
│    points_balance   │
│    created_at       │           ┌──────────────────────┐
└─────────────────────┘           │       SIZE           │
         ▲                        ├──────────────────────┤
         │                        │ PK: size_id          │
         │ 1:M                    │    label (U)         │
         │                        └──────────────────────┘
    ┌────┴────────────────────────┐
    │        1:M                   │ 1:M      ┌──────────────────┐
    │                              └─────────┤ ITEM_CONDITION   │
    │                                        ├──────────────────┤
    │                                        │ PK: condition_id │
    │                                        │    label (U)     │
    │                                        └──────────────────┘
    │
┌───┴────────────────────────────────────────┐
│            ITEMS                           │
├────────────────────────────────────────────┤
│ PK: item_id                                │
│ FK: owner_user_id ──────────┐              │
│ FK: category_id ────────────┤──────────────┤
│ FK: size_id ────────────────┤──────────────┤
│ FK: condition_id ────────────┤─────────────┤
│    item_name                │              │
│    brand                    │              │
│    description              │              │
│    points_value             │              │
│    status (AVAILABLE/...)   │              │
│    created_at               │              │
└──────────────┬──────────────┘              │
               │                             │
               │ 1:M                         │
               │                             │
        ┌──────┴──────────┐                  │
        │  ITEM_IMAGES    │                  │
        ├─────────────────┤                  │
        │ PK: image_id    │                  │
        │ FK: item_id ────┼───────────────--─┘
        │    image_url    │
        └─────────────────┘


┌─────────────────────────┐
│      EXCHANGES          │
├─────────────────────────┤
│ PK: exchange_id         │
│ FK: requester_user_id ──┼─────────┐
│ FK: owner_user_id ──────┼─────────┤ (references USERS)
│    status (PENDING/...) │         │
│    created_at           │         │
└────────┬────────────────┘         │
         │                          │
         │ 1:M                      │
         │                          │
    ┌────┴──────────────────────────┘
    │
    ├──────────────────────────┐
    │                          │ 1:M
    │                          ▼
    │              ┌──────────────────────┐
    │              │  EXCHANGE_MESSAGES   │
    │              ├──────────────────────┤
    │              │ PK: message_id       │
    │              │ FK: exchange_id ─────┼──────┐
    │              │ FK: sender_user_id ──┼──┐   │
    │              │    message_text      │  │   │
    │              │    sent_at           │  │   │
    │              └──────────────────────┘  │   │
    │                                        │   │
    │                                    (refs USERS)
    │
    ├──────────────────────────┐
    │                          │ 1:M
    │                          ▼
    │              ┌──────────────────────┐
    │              │  EXCHANGE_ITEMS      │
    │              ├──────────────────────┤
    │              │ PK: exchange_item_id │
    │              │ FK: exchange_id ─────┼──────┐
    │              │ FK: item_id ─────────┼──┐   │
    │              └──────────────────────┘  │   │
    │                                        │   │
    │                                    (refs ITEMS)
    │
    └──────────────────────────┐
                               │ 1:1
                               ▼
                   ┌──────────────────────┐
                   │     SHIPPING         │
                   ├──────────────────────┤
                   │ PK: shipping_id      │
                   │ FK: exchange_id ─────┼──────┐
                   │    address_line      │      │
                   │    status            │      │
                   └──────────────────────┘   (unique)

┌─────────────────────────┐
│     REVIEWS             │
├─────────────────────────┤
│ PK: review_id           │
│ FK: exchange_id ────────┼─────────┐
│ FK: reviewer_user_id ───┼────┐    │
│    rating               │    │    │
│    comment              │    │    │
│    created_at           │    │    │
└─────────────────────────┘    │    │
                          (refs USERS, EXCHANGES)

┌─────────────────────────┐
│   POINTS_LOG            │
├─────────────────────────┤
│ PK: log_id              │
│ FK: user_id ────────────┼─────────┐
│    delta (±points)      │         │
│    reason               │         │
│ FK: related_exchange_id │    (refs USERS)
│    created_at           │
└─────────────────────────┘
```

## Key Relationships

| Parent Table | Child Table | Cardinality | Type | Description |
|--------------|------------|-------------|------|-------------|
| USERS | ITEMS | 1:M | Ownership | Each user owns multiple items |
| USERS | EXCHANGES | 1:M | Requester | Each user can request multiple exchanges |
| USERS | EXCHANGES | 1:M | Owner | Each user receives multiple exchange requests |
| USERS | EXCHANGE_MESSAGES | 1:M | Sender | Each user sends multiple messages |
| USERS | REVIEWS | 1:M | Reviewer | Each user can write multiple reviews |
| USERS | POINTS_LOG | 1:M | Scoring | Each user has multiple point transactions |
| ITEMS | ITEM_IMAGES | 1:M | Gallery | Each item can have multiple images |
| ITEMS | EXCHANGE_ITEMS | 1:M | Listing | Each item can appear in multiple exchanges |
| CATEGORY | ITEMS | 1:M | Classification | Each category has multiple items |
| SIZE | ITEMS | 1:M | Classification | Each size has multiple items |
| ITEM_CONDITION | ITEMS | 1:M | Classification | Each condition has multiple items |
| EXCHANGES | EXCHANGE_ITEMS | 1:M | Content | Each exchange has multiple items |
| EXCHANGES | EXCHANGE_MESSAGES | 1:M | Communication | Each exchange has multiple messages |
| EXCHANGES | SHIPPING | 1:1 | Delivery | Each exchange has one shipping record |
| EXCHANGES | REVIEWS | 1:M | Feedback | Each exchange can have multiple reviews |

## Database Design Features

### Normalization Status: **3rd Normal Form (3NF)**
- ✅ **1NF**: All attributes are atomic (no repeating groups)
- ✅ **2NF**: Non-key attributes are fully dependent on primary keys
- ✅ **3NF**: No transitive dependencies between non-key attributes
- ✅ **BCNF**: All determinants are candidate keys

### Integrity Constraints
- **Primary Keys**: All tables have surrogate primary keys (auto-increment)
- **Foreign Keys**: Enforced relationships with referential integrity
- **Unique Constraints**: username (USERS), name (CATEGORY), label (SIZE, ITEM_CONDITION)
- **Check Constraints**: status values (AVAILABLE, PENDING, COMPLETED, REJECTED)
- **Cascade Delete**: Images, Messages, Items cascade delete with parent

### Indexes
- `idx_users_username`: B-tree index on username for fast login queries
- Implicit indexes on primary keys and foreign keys

### Data Types & Storage
- **INT**: Integer IDs, points, ratings
- **VARCHAR**: Text fields with appropriate length limits
- **TEXT**: Large text content (descriptions, messages)
- **TIMESTAMP**: Automatic created_at tracking
- **Character Set**: UTF-8MB4 for emoji/international support

