USE iplate;

CREATE TABLE IF NOT EXISTS iplate_users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    api_token CHAR(64) NOT NULL UNIQUE,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_user_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'user_id'
);

SET @sql := IF(
    @has_user_id = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN user_id INT UNSIGNED NOT NULL DEFAULT 1 AFTER id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_operator_name := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'operator_name'
);

SET @sql := IF(
    @has_operator_name = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN operator_name VARCHAR(120) NOT NULL DEFAULT ''Administrador'' AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_plate_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'plate_photo_path'
);

SET @sql := IF(
    @has_plate_photo_path = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN plate_photo_path VARCHAR(255) DEFAULT NULL AFTER model',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_front_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'front_photo_path'
);

SET @sql := IF(
    @has_front_photo_path = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN front_photo_path VARCHAR(255) DEFAULT NULL AFTER plate_photo_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_driver_side_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'driver_side_photo_path'
);

SET @sql := IF(
    @has_driver_side_photo_path = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN driver_side_photo_path VARCHAR(255) DEFAULT NULL AFTER front_photo_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_rear_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'rear_photo_path'
);

SET @sql := IF(
    @has_rear_photo_path = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN rear_photo_path VARCHAR(255) DEFAULT NULL AFTER driver_side_photo_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_passenger_side_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'passenger_side_photo_path'
);

SET @sql := IF(
    @has_passenger_side_photo_path = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN passenger_side_photo_path VARCHAR(255) DEFAULT NULL AFTER rear_photo_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_client_timestamp := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'client_timestamp'
);

SET @sql := IF(
    @has_client_timestamp = 0,
    'ALTER TABLE iplate_vehicle_entries ADD COLUMN client_timestamp VARCHAR(40) DEFAULT NULL AFTER passenger_side_photo_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_vehicle_photo_path := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND COLUMN_NAME = 'vehicle_photo_path'
);

SET @sql := IF(
    @has_vehicle_photo_path = 1,
    'UPDATE iplate_vehicle_entries SET front_photo_path = COALESCE(front_photo_path, vehicle_photo_path) WHERE vehicle_photo_path IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_user_id := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND INDEX_NAME = 'idx_user_id'
);

SET @sql := IF(
    @has_idx_user_id = 0,
    'ALTER TABLE iplate_vehicle_entries ADD INDEX idx_user_id (user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'iplate_vehicle_entries'
      AND CONSTRAINT_NAME = 'fk_iplate_vehicle_entries_user'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

SET @sql := IF(
    @has_fk = 0,
    'ALTER TABLE iplate_vehicle_entries ADD CONSTRAINT fk_iplate_vehicle_entries_user FOREIGN KEY (user_id) REFERENCES iplate_users(id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO iplate_users (id, name, username, password_hash, api_token)
SELECT
    1,
    'Administrador',
    'admin',
    '$2y$10$Z.qGcQ1cxSCunthlaJpgO.r4VZN0ONqHLeqvfas3dwrhKY4VHr2a.',
    SHA2(UUID(), 256)
WHERE NOT EXISTS (
    SELECT 1 FROM iplate_users WHERE username = 'admin'
);
