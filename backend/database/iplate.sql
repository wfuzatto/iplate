CREATE DATABASE IF NOT EXISTS iplate
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS iplate_vehicle_entries (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    operator_name VARCHAR(120) NOT NULL,
    plate VARCHAR(20) NOT NULL,
    reservation VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    plate_photo_path VARCHAR(255) DEFAULT NULL,
    front_photo_path VARCHAR(255) DEFAULT NULL,
    driver_side_photo_path VARCHAR(255) DEFAULT NULL,
    rear_photo_path VARCHAR(255) DEFAULT NULL,
    passenger_side_photo_path VARCHAR(255) DEFAULT NULL,
    client_timestamp VARCHAR(40) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_iplate_vehicle_entries_user FOREIGN KEY (user_id) REFERENCES iplate_users (id),
    INDEX idx_user_id (user_id),
    INDEX idx_plate (plate),
    INDEX idx_reservation (reservation),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
