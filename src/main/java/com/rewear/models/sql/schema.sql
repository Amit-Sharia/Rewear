-- ReWear MySQL schema aligned with the Java desktop client (JDK 17 + JDBC).
-- Database name: rewear
-- Run: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS rewear CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rewear;

CREATE TABLE IF NOT EXISTS USERS (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  points_balance INT NOT NULL DEFAULT 100,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_users_username (username)
);

-- ---------- Core lookups ----------
CREATE TABLE IF NOT EXISTS CATEGORY (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS SIZE (
  size_id INT AUTO_INCREMENT PRIMARY KEY,
  label VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS ITEM_CONDITION (
  condition_id INT AUTO_INCREMENT PRIMARY KEY,
  label VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS ITEMS (
  item_id INT AUTO_INCREMENT PRIMARY KEY,
  owner_user_id INT NOT NULL,
  category_id INT NOT NULL,
  size_id INT NOT NULL,
  condition_id INT NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  brand VARCHAR(128) NOT NULL DEFAULT '',
  description TEXT,
  points_value INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_items_owner FOREIGN KEY (owner_user_id) REFERENCES USERS (user_id),
  CONSTRAINT fk_items_cat FOREIGN KEY (category_id) REFERENCES CATEGORY (category_id),
  CONSTRAINT fk_items_size FOREIGN KEY (size_id) REFERENCES SIZE (size_id),
  CONSTRAINT fk_items_cond FOREIGN KEY (condition_id) REFERENCES ITEM_CONDITION (condition_id)
);

CREATE TABLE IF NOT EXISTS ITEM_IMAGES (
  image_id INT AUTO_INCREMENT PRIMARY KEY,
  item_id INT NOT NULL,
  image_url VARCHAR(1024) NOT NULL,
  CONSTRAINT fk_img_item FOREIGN KEY (item_id) REFERENCES ITEMS (item_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS EXCHANGES (
  exchange_id INT AUTO_INCREMENT PRIMARY KEY,
  requester_user_id INT NOT NULL,
  owner_user_id INT NOT NULL,
  -- PENDING -> (ACCEPTED flow) COMPlETED, or REJECTED
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ex_req FOREIGN KEY (requester_user_id) REFERENCES USERS (user_id),
  CONSTRAINT fk_ex_own FOREIGN KEY (owner_user_id) REFERENCES USERS (user_id)
);

CREATE TABLE IF NOT EXISTS EXCHANGE_ITEMS (
  exchange_item_id INT AUTO_INCREMENT PRIMARY KEY,
  exchange_id INT NOT NULL,
  item_id INT NOT NULL,
  CONSTRAINT fk_ei_ex FOREIGN KEY (exchange_id) REFERENCES EXCHANGES (exchange_id) ON DELETE CASCADE,
  CONSTRAINT fk_ei_item FOREIGN KEY (item_id) REFERENCES ITEMS (item_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS SHIPPING (
  shipping_id INT AUTO_INCREMENT PRIMARY KEY,
  exchange_id INT NOT NULL,
  address_line VARCHAR(512) NOT NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
  CONSTRAINT fk_ship_ex FOREIGN KEY (exchange_id) REFERENCES EXCHANGES (exchange_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS EXCHANGE_MESSAGES (
  message_id INT AUTO_INCREMENT PRIMARY KEY,
  exchange_id INT NOT NULL,
  sender_user_id INT NOT NULL,
  message_text TEXT NOT NULL,
  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_msg_ex FOREIGN KEY (exchange_id) REFERENCES EXCHANGES (exchange_id) ON DELETE CASCADE,
  CONSTRAINT fk_msg_user FOREIGN KEY (sender_user_id) REFERENCES USERS (user_id)
);

CREATE TABLE IF NOT EXISTS REVIEWS (
  review_id INT AUTO_INCREMENT PRIMARY KEY,
  exchange_id INT NOT NULL,
  reviewer_user_id INT NOT NULL,
  rating INT NOT NULL,
  comment TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rev_ex FOREIGN KEY (exchange_id) REFERENCES EXCHANGES (exchange_id) ON DELETE CASCADE,
  CONSTRAINT fk_rev_user FOREIGN KEY (reviewer_user_id) REFERENCES USERS (user_id),
  UNIQUE KEY uq_review_once (exchange_id, reviewer_user_id)
);

CREATE TABLE IF NOT EXISTS POINTS_LOG (
  log_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  delta INT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  related_exchange_id INT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pl_user FOREIGN KEY (user_id) REFERENCES USERS (user_id),
  CONSTRAINT fk_pl_ex FOREIGN KEY (related_exchange_id) REFERENCES EXCHANGES (exchange_id) ON DELETE SET NULL
);

-- ---------- Sample data ----------
INSERT INTO USERS (username, email, password_hash, points_balance)
VALUES
  ('alice', 'alice@example.com', 'alice123', 500),
  ('bob', 'bob@example.com', 'bob123', 300);

INSERT INTO CATEGORY (name) VALUES
  ('Tops'), ('Bottoms'), ('Outerwear'), ('Shoes'), ('Accessories')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO SIZE (label) VALUES
  ('XS'), ('S'), ('M'), ('L'), ('XL')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO ITEM_CONDITION (label) VALUES
  ('New'), ('Like new'), ('Good'), ('Fair')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- Items owned by alice (user_id 1) and bob (user_id 2) — adjust IDs if your auto_increment differs.
INSERT INTO ITEMS (owner_user_id, category_id, size_id, condition_id, item_name, brand, description, points_value, status)
SELECT u.user_id, c.category_id, s.size_id, ic.condition_id,
       'Denim jacket', 'ReWear Basics', 'Light wear, classic cut', 80, 'AVAILABLE'
FROM USERS u, CATEGORY c, SIZE s, ITEM_CONDITION ic
WHERE u.username = 'alice' AND c.name = 'Outerwear' AND s.label = 'M' AND ic.label = 'Good'
LIMIT 1;

INSERT INTO ITEMS (owner_user_id, category_id, size_id, condition_id, item_name, brand, description, points_value, status)
SELECT u.user_id, c.category_id, s.size_id, ic.condition_id,
       'Running shoes', 'Stride', 'Minor sole wear', 120, 'AVAILABLE'
FROM USERS u, CATEGORY c, SIZE s, ITEM_CONDITION ic
WHERE u.username = 'bob' AND c.name = 'Shoes' AND s.label = 'L' AND ic.label = 'Like new'
LIMIT 1;

INSERT INTO ITEM_IMAGES (item_id, image_url)
SELECT item_id, 'https://example.com/images/sample1.jpg' FROM ITEMS WHERE item_name = 'Denim jacket' LIMIT 1;
