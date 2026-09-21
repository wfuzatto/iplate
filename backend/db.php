<?php

declare(strict_types=1);

$config = require __DIR__ . '/config.php';
date_default_timezone_set($config['app']['timezone']);

$db = $config['db'];
$dsn = sprintf(
    'mysql:host=%s;port=%d;dbname=%s;charset=%s',
    $db['host'],
    $db['port'],
    $db['database'],
    $db['charset']
);

const TABLE_USERS = 'iplate_users';
const TABLE_VEHICLE_ENTRIES = 'iplate_vehicle_entries';

function createPdo(string $dsn, array $db): PDO
{
    $options = [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ];

    if (defined('PDO::MYSQL_ATTR_USE_BUFFERED_QUERY')) {
        $options[PDO::MYSQL_ATTR_USE_BUFFERED_QUERY] = true;
    }

    return new PDO($dsn, $db['username'], $db['password'], $options);
}

function executeSqlStatements(PDO $pdo, string $sql): void
{
    $statements = array_filter(array_map('trim', explode(';', $sql)));
    foreach ($statements as $statement) {
        $pdo->exec($statement);
    }
}

function bootstrapDatabase(array $db): void
{
    $serverDsn = sprintf(
        'mysql:host=%s;port=%d;charset=%s',
        $db['host'],
        $db['port'],
        $db['charset']
    );

    $schemaFile = __DIR__ . '/database/iplate.sql';
    if (!is_file($schemaFile)) {
        throw new RuntimeException('Schema do banco nao encontrado em backend/database/iplate.sql.');
    }

    $schemaSql = file_get_contents($schemaFile);
    if ($schemaSql === false) {
        throw new RuntimeException('Nao foi possivel ler o schema do banco.');
    }

    $pdoBootstrap = createPdo($serverDsn, $db);
    executeSqlStatements($pdoBootstrap, $schemaSql);
}

function syncDatabaseSchema(PDO $pdo): bool
{
    $changed = false;
    migrateLegacyTableNames($pdo);

    if (!tableExists($pdo, TABLE_USERS)) {
        $pdo->exec(
            'CREATE TABLE ' . TABLE_USERS . ' (
                id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(120) NOT NULL,
                username VARCHAR(80) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                api_token CHAR(64) NOT NULL UNIQUE,
                is_active TINYINT(1) NOT NULL DEFAULT 1,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
        );
        $changed = true;
    }

    if (!tableExists($pdo, TABLE_VEHICLE_ENTRIES)) {
        $pdo->exec(
            'CREATE TABLE ' . TABLE_VEHICLE_ENTRIES . ' (
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
                INDEX idx_user_id (user_id),
                INDEX idx_plate (plate),
                INDEX idx_reservation (reservation),
                INDEX idx_created_at (created_at),
                CONSTRAINT fk_iplate_vehicle_entries_user FOREIGN KEY (user_id) REFERENCES ' . TABLE_USERS . '(id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
        );
        $changed = true;
    }

    $columnDefinitions = [
        'user_id' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN user_id INT UNSIGNED NOT NULL DEFAULT 1 AFTER id',
        'operator_name' => "ALTER TABLE " . TABLE_VEHICLE_ENTRIES . " ADD COLUMN operator_name VARCHAR(120) NOT NULL DEFAULT 'Administrador' AFTER user_id",
        'plate_photo_path' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN plate_photo_path VARCHAR(255) DEFAULT NULL AFTER model',
        'front_photo_path' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN front_photo_path VARCHAR(255) DEFAULT NULL AFTER plate_photo_path',
        'driver_side_photo_path' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN driver_side_photo_path VARCHAR(255) DEFAULT NULL AFTER front_photo_path',
        'rear_photo_path' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN rear_photo_path VARCHAR(255) DEFAULT NULL AFTER driver_side_photo_path',
        'passenger_side_photo_path' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN passenger_side_photo_path VARCHAR(255) DEFAULT NULL AFTER rear_photo_path',
        'client_timestamp' => 'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD COLUMN client_timestamp VARCHAR(40) DEFAULT NULL AFTER passenger_side_photo_path',
    ];

    foreach ($columnDefinitions as $column => $statement) {
        if (!columnExists($pdo, TABLE_VEHICLE_ENTRIES, $column)) {
            $pdo->exec($statement);
            $changed = true;
        }
    }

    if (columnExists($pdo, TABLE_VEHICLE_ENTRIES, 'vehicle_photo_path')) {
        $pdo->exec(
            'UPDATE ' . TABLE_VEHICLE_ENTRIES . '
             SET front_photo_path = COALESCE(front_photo_path, vehicle_photo_path)
             WHERE vehicle_photo_path IS NOT NULL'
        );
    }

    if (!indexExists($pdo, TABLE_VEHICLE_ENTRIES, 'idx_user_id')) {
        $pdo->exec('ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . ' ADD INDEX idx_user_id (user_id)');
        $changed = true;
    }

    $hasNewForeignKey = foreignKeyExists($pdo, TABLE_VEHICLE_ENTRIES, 'fk_iplate_vehicle_entries_user');
    $hasLegacyForeignKey = foreignKeyExists($pdo, TABLE_VEHICLE_ENTRIES, 'fk_vehicle_entries_user');
    if (!$hasNewForeignKey && !$hasLegacyForeignKey) {
        $pdo->exec(
            'ALTER TABLE ' . TABLE_VEHICLE_ENTRIES . '
             ADD CONSTRAINT fk_iplate_vehicle_entries_user FOREIGN KEY (user_id) REFERENCES ' . TABLE_USERS . '(id)'
        );
        $changed = true;
    }

    $adminExistsStmt = $pdo->prepare('SELECT COUNT(*) FROM ' . TABLE_USERS . ' WHERE username = :username');
    $adminExistsStmt->execute([':username' => 'admin']);
    $adminExists = (int) $adminExistsStmt->fetchColumn() > 0;
    $adminExistsStmt->closeCursor();
    if (!$adminExists) {
        $insertAdminStmt = $pdo->prepare(
            'INSERT INTO ' . TABLE_USERS . ' (id, name, username, password_hash, api_token)
             VALUES (:id, :name, :username, :password_hash, :api_token)'
        );
        $insertAdminStmt->execute([
            ':id' => 1,
            ':name' => 'Administrador',
            ':username' => 'admin',
            ':password_hash' => '$2y$10$Z.qGcQ1cxSCunthlaJpgO.r4VZN0ONqHLeqvfas3dwrhKY4VHr2a.',
            ':api_token' => bin2hex(random_bytes(32)),
        ]);
        $insertAdminStmt->closeCursor();
        $changed = true;
    }

    return $changed;
}

function migrateLegacyTableNames(PDO $pdo): void
{
    if (tableExists($pdo, 'users') && !tableExists($pdo, TABLE_USERS)) {
        $pdo->exec('RENAME TABLE users TO ' . TABLE_USERS);
    }

    if (tableExists($pdo, 'vehicle_entries') && !tableExists($pdo, TABLE_VEHICLE_ENTRIES)) {
        $pdo->exec('RENAME TABLE vehicle_entries TO ' . TABLE_VEHICLE_ENTRIES);
    }
}

function tableExists(PDO $pdo, string $tableName): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*)
         FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = :table_name'
    );
    $stmt->execute([':table_name' => $tableName]);
    $exists = (int) $stmt->fetchColumn() > 0;
    $stmt->closeCursor();

    return $exists;
}

function columnExists(PDO $pdo, string $tableName, string $columnName): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*)
         FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = :table_name
           AND COLUMN_NAME = :column_name'
    );
    $stmt->execute([
        ':table_name' => $tableName,
        ':column_name' => $columnName,
    ]);
    $exists = (int) $stmt->fetchColumn() > 0;
    $stmt->closeCursor();

    return $exists;
}

function indexExists(PDO $pdo, string $tableName, string $indexName): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*)
         FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = :table_name
           AND INDEX_NAME = :index_name'
    );
    $stmt->execute([
        ':table_name' => $tableName,
        ':index_name' => $indexName,
    ]);
    $exists = (int) $stmt->fetchColumn() > 0;
    $stmt->closeCursor();

    return $exists;
}

function foreignKeyExists(PDO $pdo, string $tableName, string $constraintName): bool
{
    $stmt = $pdo->prepare(
        'SELECT COUNT(*)
         FROM information_schema.TABLE_CONSTRAINTS
         WHERE CONSTRAINT_SCHEMA = DATABASE()
           AND TABLE_NAME = :table_name
           AND CONSTRAINT_NAME = :constraint_name
           AND CONSTRAINT_TYPE = "FOREIGN KEY"'
    );
    $stmt->execute([
        ':table_name' => $tableName,
        ':constraint_name' => $constraintName,
    ]);
    $exists = (int) $stmt->fetchColumn() > 0;
    $stmt->closeCursor();

    return $exists;
}

try {
    $pdo = createPdo($dsn, $db);
    syncDatabaseSchema($pdo);
    $pdo = null;
    $pdo = createPdo($dsn, $db);
} catch (PDOException $exception) {
    if (($exception->errorInfo[1] ?? null) === 1049) {
        try {
            bootstrapDatabase($db);
            $pdo = createPdo($dsn, $db);
            syncDatabaseSchema($pdo);
            $pdo = null;
            $pdo = createPdo($dsn, $db);
        } catch (Throwable $bootstrapException) {
            http_response_code(500);
            exit('Erro ao criar o banco de dados automaticamente: ' . $bootstrapException->getMessage());
        }
    }

    if (!isset($pdo)) {
        http_response_code(500);
        exit('Erro ao conectar ao banco de dados: ' . $exception->getMessage());
    }
} catch (Throwable $throwable) {
    error_log('[iPlate] DB bootstrap/sync error: ' . $throwable->getMessage());
    http_response_code(500);
    exit('Erro interno ao inicializar banco de dados do iPlate.');
}
