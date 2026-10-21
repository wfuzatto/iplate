<?php

return [
    'db' => [
        'host' => '127.0.0.1',
        'port' => 3306,
        'database' => 'iplate',
        'username' => 'root',
        'password' => '',
        'charset' => 'utf8mb4',
    ],
    // Segredos reais ficam apenas em config.local.php (ignorado no Git).
    'reservation' => [
        'provider' => 'expresso',
        'suggestions_source' => 'none',
    ],
    'expresso' => [
        'cpf' => '',
        'password' => '',
        'token_url' => 'https://vale.expresso.app/api/obter_token',
        'reservation_url' => 'https://vale.expresso.app/api/reserva',
        'timeout_seconds' => 20,
    ],
    // Opcional e legado: habilite apenas se as tabelas antigas ainda existirem.
    'hotelaria_db' => [
        'host' => '127.0.0.1',
        'port' => 3306,
        'database' => 'hotelaria',
        'username' => 'root',
        'password' => '',
        'charset' => 'utf8mb4',
    ],
];
