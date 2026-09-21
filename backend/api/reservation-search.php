<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

require __DIR__ . '/../db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'items' => [], 'message' => 'Metodo nao permitido.']);
    exit;
}

$term = trim((string) ($_GET['term'] ?? ''));
$apiToken = trim((string) ($_GET['api_token'] ?? ''));
$exact = (string) ($_GET['exact'] ?? '') === '1';

if (mb_strlen($term) < 3) {
    echo json_encode(['success' => true, 'items' => []]);
    exit;
}

if ($apiToken === '') {
    http_response_code(401);
    echo json_encode(['success' => false, 'items' => [], 'message' => 'Usuario nao autenticado.']);
    exit;
}

$userStmt = $pdo->prepare('SELECT id FROM ' . TABLE_USERS . ' WHERE api_token = :api_token AND is_active = 1 LIMIT 1');
$userStmt->execute([':api_token' => $apiToken]);
$user = $userStmt->fetch();

if (!$user) {
    http_response_code(401);
    echo json_encode(['success' => false, 'items' => [], 'message' => 'Sessao do usuario invalida.']);
    exit;
}

$config = require __DIR__ . '/../config.php';
$hotelariaDb = $config['hotelaria_db'];

$hotelariaDsn = sprintf(
    'mysql:host=%s;port=%d;dbname=%s;charset=%s',
    $hotelariaDb['host'],
    $hotelariaDb['port'],
    $hotelariaDb['database'],
    $hotelariaDb['charset']
);

try {
    $hotelariaPdo = new PDO($hotelariaDsn, $hotelariaDb['username'], $hotelariaDb['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);

    if ($exact) {
        $stmt = $hotelariaPdo->prepare(
            'SELECT
                r.id AS reserva_id,
                r.codigo,
                r.status,
                r.data_checkin,
                r.data_checkout,
                COUNT(DISTINCT h.id) AS guest_count,
                GROUP_CONCAT(DISTINCT h.nome ORDER BY h.nome SEPARATOR ", ") AS hospedes
             FROM reservas r
             LEFT JOIN reserva_hospedes rh ON rh.reserva_id = r.id AND rh.cliente_id = r.cliente_id
             LEFT JOIN hospedes h ON h.id = rh.hospede_id
             WHERE r.codigo = :codigo
               AND r.status IN ("confirmada", "checkin")
             GROUP BY r.id, r.codigo, r.status, r.data_checkin, r.data_checkout
             LIMIT 1'
        );
        $stmt->execute([':codigo' => $term]);
    } else {
        $like = '%' . $term . '%';
        $stmt = $hotelariaPdo->prepare(
            'SELECT
                r.id AS reserva_id,
                r.codigo,
                r.status,
                r.data_checkin,
                r.data_checkout,
                COUNT(DISTINCT h.id) AS guest_count,
                GROUP_CONCAT(DISTINCT h.nome ORDER BY h.nome SEPARATOR ", ") AS hospedes
             FROM reservas r
             LEFT JOIN reserva_hospedes rh ON rh.reserva_id = r.id AND rh.cliente_id = r.cliente_id
             LEFT JOIN hospedes h ON h.id = rh.hospede_id
             WHERE r.data_checkin = CURDATE()
               AND r.status IN ("confirmada", "checkin")
               AND (
                    r.codigo LIKE :like_codigo
                    OR COALESCE(h.nome, "") LIKE :like_nome
               )
             GROUP BY r.id, r.codigo, r.status, r.data_checkin, r.data_checkout
             ORDER BY
                CASE WHEN r.status = "checkin" THEN 0 ELSE 1 END,
                r.codigo ASC
             LIMIT 25'
        );
        $stmt->execute([
            ':like_codigo' => $like,
            ':like_nome' => $like,
        ]);
    }

    $items = array_map(static function (array $row): array {
        return [
            'reservation_id' => (int) ($row['reserva_id'] ?? 0),
            'reservation_code' => (string) ($row['codigo'] ?? ''),
            'guest_name' => (string) ($row['hospedes'] ?? ''),
            'checkin_date' => (string) ($row['data_checkin'] ?? ''),
            'checkout_date' => (string) ($row['data_checkout'] ?? ''),
            'guest_count' => (int) ($row['guest_count'] ?? 0),
            'status' => (string) ($row['status'] ?? ''),
        ];
    }, $stmt->fetchAll());

    echo json_encode([
        'success' => true,
        'items' => $items,
    ]);
} catch (Throwable $throwable) {
    echo json_encode([
        'success' => true,
        'items' => [],
        'message' => 'Busca indisponivel no momento: ' . $throwable->getMessage(),
    ]);
}
