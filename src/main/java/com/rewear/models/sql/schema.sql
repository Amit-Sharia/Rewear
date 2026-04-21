-- ReWear MySQL Phase 2 Complete Schema
-- Base tables + Advanced Features (Functions, Procedure, Triggers)
-- Database: rewear
-- Usage: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS rewear CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rewear;

-- ---------- Phase 2: Drop advanced objects for clean re-run ----------
DROP PROCEDURE IF EXISTS sp_complete_exchange;
DROP FUNCTION IF EXISTS fn_owner_average_rating;
DROP FUNCTION IF EXISTS fn_user_available_items;
DROP TRIGGER IF EXISTS trg_points_log_apply_balance;
DROP TRIGGER IF EXISTS trg_points_log_prevent_negative;
DROP TRIGGER IF EXISTS trg_reviews_rating_check;

-- ---------- Core Tables ----------
CREATE TABLE IF NOT EXISTS `USERS` (
  `user_id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) NOT NULL UNIQUE,
  `email` VARCHAR(255) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `points_balance` INT NOT NULL DEFAULT 100,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_users_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `CATEGORY` (
  `category_id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS `SIZE` (
  `size_id` INT AUTO_INCREMENT PRIMARY KEY,
  `label` VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS `ITEM_CONDITION` (
  `condition_id` INT AUTO_INCREMENT PRIMARY KEY,
  `label` VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS `ITEMS` (
  `item_id` INT AUTO_INCREMENT PRIMARY KEY,
  `owner_user_id` INT NOT NULL,
  `category_id` INT NOT NULL,
  `size_id` INT NOT NULL,
  `condition_id` INT NOT NULL,
  `item_name` VARCHAR(255) NOT NULL,
  `brand` VARCHAR(128) NOT NULL DEFAULT '',
  `description` TEXT,
  `points_value` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_items_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `USERS` (`user_id`),
  CONSTRAINT `fk_items_cat` FOREIGN KEY (`category_id`) REFERENCES `CATEGORY` (`category_id`),
  CONSTRAINT `fk_items_size` FOREIGN KEY (`size_id`) REFERENCES `SIZE` (`size_id`),
  CONSTRAINT `fk_items_cond` FOREIGN KEY (`condition_id`) REFERENCES `ITEM_CONDITION` (`condition_id`)
);

CREATE TABLE IF NOT EXISTS `ITEM_IMAGES` (
  `image_id` INT AUTO_INCREMENT PRIMARY KEY,
  `item_id` INT NOT NULL,
  `image_url` VARCHAR(1024) NOT NULL,
  CONSTRAINT `fk_img_item` FOREIGN KEY (`item_id`) REFERENCES `ITEMS` (`item_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `EXCHANGES` (
  `exchange_id` INT AUTO_INCREMENT PRIMARY KEY,
  `requester_user_id` INT NOT NULL,
  `owner_user_id` INT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_ex_req` FOREIGN KEY (`requester_user_id`) REFERENCES `USERS` (`user_id`),
  CONSTRAINT `fk_ex_own` FOREIGN KEY (`owner_user_id`) REFERENCES `USERS` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `EXCHANGE_ITEMS` (
  `exchange_item_id` INT AUTO_INCREMENT PRIMARY KEY,
  `exchange_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  CONSTRAINT `fk_ei_ex` FOREIGN KEY (`exchange_id`) REFERENCES `EXCHANGES` (`exchange_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ei_item` FOREIGN KEY (`item_id`) REFERENCES `ITEMS` (`item_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `SHIPPING` (
  `shipping_id` INT AUTO_INCREMENT PRIMARY KEY,
  `exchange_id` INT NOT NULL,
  `address_line` VARCHAR(512) NOT NULL,
  `status` VARCHAR(64) NOT NULL DEFAULT 'PENDING',
  CONSTRAINT `fk_ship_ex` FOREIGN KEY (`exchange_id`) REFERENCES `EXCHANGES` (`exchange_id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `EXCHANGE_MESSAGES` (
  `message_id` INT AUTO_INCREMENT PRIMARY KEY,
  `exchange_id` INT NOT NULL,
  `sender_user_id` INT NOT NULL,
  `message_text` TEXT NOT NULL,
  `sent_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_msg_ex` FOREIGN KEY (`exchange_id`) REFERENCES `EXCHANGES` (`exchange_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_user` FOREIGN KEY (`sender_user_id`) REFERENCES `USERS` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `REVIEWS` (
  `review_id` INT AUTO_INCREMENT PRIMARY KEY,
  `exchange_id` INT NOT NULL,
  `reviewer_user_id` INT NOT NULL,
  `rating` INT NOT NULL,
  `comment` TEXT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_rev_ex` FOREIGN KEY (`exchange_id`) REFERENCES `EXCHANGES` (`exchange_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rev_user` FOREIGN KEY (`reviewer_user_id`) REFERENCES `USERS` (`user_id`),
  UNIQUE KEY `uq_review_once` (`exchange_id`, `reviewer_user_id`)
);

CREATE TABLE IF NOT EXISTS `POINTS_LOG` (
  `log_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `delta` INT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `related_exchange_id` INT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_pl_user` FOREIGN KEY (`user_id`) REFERENCES `USERS` (`user_id`),
  CONSTRAINT `fk_pl_ex` FOREIGN KEY (`related_exchange_id`) REFERENCES `EXCHANGES` (`exchange_id`) ON DELETE SET NULL
);

-- ---------- Phase 2 Advanced Features ----------
DELIMITER $$

CREATE FUNCTION `fn_user_available_items`(p_user_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
  DECLARE v_count INT DEFAULT 0;

  SELECT COUNT(*)
    INTO v_count
  FROM ITEMS
  WHERE owner_user_id = p_user_id
    AND status = 'AVAILABLE';

  RETURN v_count;
END$$

CREATE FUNCTION `fn_owner_average_rating`(p_owner_user_id INT)
RETURNS DECIMAL(4,2)
DETERMINISTIC
READS SQL DATA
BEGIN
  DECLARE v_avg_rating DECIMAL(4,2);

  SELECT AVG(r.rating)
    INTO v_avg_rating
  FROM REVIEWS r
  JOIN EXCHANGES e ON e.exchange_id = r.exchange_id
  WHERE e.owner_user_id = p_owner_user_id;

  RETURN IFNULL(v_avg_rating, 0.00);
END$$

CREATE PROCEDURE `sp_complete_exchange`(IN p_exchange_id INT)
BEGIN
  DECLARE v_requester_id INT;
  DECLARE v_owner_id INT;
  DECLARE v_status VARCHAR(32);
  DECLARE v_total_points INT DEFAULT 0;

  START TRANSACTION;

  SELECT requester_user_id, owner_user_id, status
    INTO v_requester_id, v_owner_id, v_status
  FROM EXCHANGES
  WHERE exchange_id = p_exchange_id
  FOR UPDATE;

  IF v_requester_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Exchange not found';
  END IF;

  IF v_status <> 'ACCEPTED' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Only ACCEPTED exchanges can be completed';
  END IF;

  SELECT IFNULL(SUM(i.points_value), 0)
    INTO v_total_points
  FROM EXCHANGE_ITEMS ei
  JOIN ITEMS i ON i.item_id = ei.item_id
  WHERE ei.exchange_id = p_exchange_id
  FOR UPDATE;

  INSERT INTO POINTS_LOG (user_id, delta, reason, related_exchange_id)
  VALUES
    (v_requester_id, -v_total_points, 'Points spent for completed exchange', p_exchange_id),
    (v_owner_id, v_total_points, 'Points earned from completed exchange', p_exchange_id);

  UPDATE ITEMS i
  JOIN EXCHANGE_ITEMS ei ON ei.item_id = i.item_id
  SET i.status = 'EXCHANGED'
  WHERE ei.exchange_id = p_exchange_id;

  UPDATE EXCHANGES
  SET status = 'COMPLETED'
  WHERE exchange_id = p_exchange_id;

  COMMIT;
END$$

CREATE TRIGGER `trg_reviews_rating_check`
BEFORE INSERT ON REVIEWS
FOR EACH ROW
BEGIN
  IF NEW.rating < 1 OR NEW.rating > 5 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Rating must be between 1 and 5';
  END IF;
END$$

CREATE TRIGGER `trg_points_log_prevent_negative`
BEFORE INSERT ON POINTS_LOG
FOR EACH ROW
BEGIN
  DECLARE v_current_balance INT;

  SELECT points_balance
    INTO v_current_balance
  FROM USERS
  WHERE user_id = NEW.user_id
  FOR UPDATE;

  IF v_current_balance + NEW.delta < 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Insufficient points balance for this transaction';
  END IF;
END$$

CREATE TRIGGER `trg_points_log_apply_balance`
AFTER INSERT ON POINTS_LOG
FOR EACH ROW
BEGIN
  UPDATE USERS
  SET points_balance = points_balance + NEW.delta
  WHERE user_id = NEW.user_id;
END$$

DELIMITER ;

-- ---------- Sample Data ----------
INSERT INTO USERS (username, email, password_hash, points_balance)
VALUES
  ('alice', 'alice@example.com', 'alice123', 500),
  ('bob', 'bob@example.com', 'bob123', 300)
ON DUPLICATE KEY UPDATE
  email = VALUES(email),
  password_hash = VALUES(password_hash),
  points_balance = VALUES(points_balance);

INSERT INTO CATEGORY (name) VALUES
  ('Tops'), ('Bottoms'), ('Outerwear'), ('Shoes'), ('Accessories')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO SIZE (label) VALUES
  ('XS'), ('S'), ('M'), ('L'), ('XL')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO ITEM_CONDITION (label) VALUES
  ('New'), ('Like new'), ('Good'), ('Fair')
ON DUPLICATE KEY UPDATE label = VALUES(label);

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

-- Sample exchange
INSERT INTO EXCHANGES (requester_user_id, owner_user_id, status)
SELECT requester.user_id, owner.user_id, 'PENDING'
FROM USERS requester
JOIN USERS owner
WHERE requester.username = 'alice'
  AND owner.username = 'bob'
  AND NOT EXISTS (
    SELECT 1
    FROM EXCHANGES e
    WHERE e.requester_user_id = requester.user_id
      AND e.owner_user_id = owner.user_id
  )
LIMIT 1;

INSERT INTO EXCHANGE_ITEMS (exchange_id, item_id)
SELECT e.exchange_id, i.item_id
FROM EXCHANGES e
JOIN USERS requester ON requester.user_id = e.requester_user_id
JOIN USERS owner ON owner.user_id = e.owner_user_id
JOIN ITEMS i ON i.owner_user_id = owner.user_id
WHERE requester.username = 'alice'
  AND owner.username = 'bob'
  AND i.item_name = 'Running shoes'
  AND NOT EXISTS (
    SELECT 1
    FROM EXCHANGE_ITEMS ei
    WHERE ei.exchange_id = e.exchange_id
      AND ei.item_id = i.item_id
  )
LIMIT 1;

-- ---------- Demo Queries ----------
/*
SELECT fn_user_available_items(1) AS available_items_for_alice;
SELECT fn_owner_average_rating(1) AS average_rating_for_alice;
UPDATE EXCHANGES SET status = 'ACCEPTED' WHERE exchange_id = 1;
CALL sp_complete_exchange(1);
INSERT INTO REVIEWS (exchange_id, reviewer_user_id, rating, comment)
VALUES (1, 2, 5, 'Smooth exchange and quick shipping');
*/
