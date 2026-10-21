<?php

declare(strict_types=1);

$hotelariaDatabaseConfig = [];
$hotelariaConfigPath = dirname(__DIR__, 2) . '/hotelaria/config/database.php';
if (is_file($hotelariaConfigPath)) {
    $loadedHotelariaConfig = require $hotelariaConfigPath;
    if (is_array($loadedHotelariaConfig)) {
        $hotelariaDatabaseConfig = $loadedHotelariaConfig;
    }
}

$localConfigPath = __DIR__ . '/config.local.php';
$localConfig = [];
if (is_file($localConfigPath)) {
    $loadedLocalConfig = require $localConfigPath;
    if (is_array($loadedLocalConfig)) {
        $localConfig = $loadedLocalConfig;
    }
}

$dbLocal = is_array($localConfig['db'] ?? null) ? $localConfig['db'] : [];
$hotelariaLocal = is_array($localConfig['hotelaria_db'] ?? null) ? $localConfig['hotelaria_db'] : [];

return [
    'db' => [
        'host' => $dbLocal['host'] ?? getenv('IPLATE_DB_HOST') ?: '127.0.0.1',
        'port' => (int) ($dbLocal['port'] ?? getenv('IPLATE_DB_PORT') ?: 3306),
        'database' => $dbLocal['database'] ?? getenv('IPLATE_DB_NAME') ?: 'iplate',
        'username' => $dbLocal['username'] ?? getenv('IPLATE_DB_USER') ?: 'root',
        'password' => $dbLocal['password'] ?? getenv('IPLATE_DB_PASSWORD') ?: '',
        'charset' => $dbLocal['charset'] ?? 'utf8mb4',
    ],
    'hotelaria_db' => [
        'host' => $hotelariaLocal['host'] ?? $hotelariaDatabaseConfig['host'] ?? getenv('HOTELARIA_DB_HOST') ?: '127.0.0.1',
        'port' => (int) ($hotelariaLocal['port'] ?? $hotelariaDatabaseConfig['port'] ?? getenv('HOTELARIA_DB_PORT') ?: 3306),
        'database' => $hotelariaLocal['database'] ?? $hotelariaDatabaseConfig['database'] ?? getenv('HOTELARIA_DB_NAME') ?: 'hotelaria',
        'username' => $hotelariaLocal['username'] ?? $hotelariaDatabaseConfig['username'] ?? getenv('HOTELARIA_DB_USER') ?: 'root',
        'password' => $hotelariaLocal['password'] ?? $hotelariaDatabaseConfig['password'] ?? getenv('HOTELARIA_DB_PASSWORD') ?: '',
        'charset' => $hotelariaLocal['charset'] ?? $hotelariaDatabaseConfig['charset'] ?? 'utf8mb4',
    ],
    'reservation' => [
        // Expresso e o provedor atual. Use hotelaria_sql somente em instalacoes antigas.
        'provider' => $localConfig['reservation']['provider'] ?? getenv('IPLATE_RESERVATION_PROVIDER') ?: 'expresso',
        // A Expresso nao oferece autocomplete por nome/codigo parcial nesse contrato.
        'suggestions_source' => $localConfig['reservation']['suggestions_source'] ?? getenv('IPLATE_RESERVATION_SUGGESTIONS') ?: 'none',
    ],
    'expresso' => [
        'cpf' => $localConfig['expresso']['cpf'] ?? getenv('IPLATE_EXPRESSO_CPF') ?: getenv('IPLATE_EXPRESSO_USER') ?: '',
        'password' => $localConfig['expresso']['password'] ?? getenv('IPLATE_EXPRESSO_PASSWORD') ?: '',
        'token_url' => $localConfig['expresso']['token_url'] ?? getenv('IPLATE_EXPRESSO_TOKEN_URL') ?: 'https://vale.expresso.app/api/obter_token',
        'reservation_url' => $localConfig['expresso']['reservation_url'] ?? getenv('IPLATE_EXPRESSO_RESERVATION_URL') ?: 'https://vale.expresso.app/api/reserva',
        'timeout_seconds' => (int) ($localConfig['expresso']['timeout_seconds'] ?? getenv('IPLATE_EXPRESSO_TIMEOUT') ?: 20),
    ],
    'app' => [
        'name' => 'iPlate',
        'base_url' => '',
        'timezone' => 'America/Sao_Paulo',
    ],
];
