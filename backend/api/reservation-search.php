<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, private');
require __DIR__ . '/../db.php';

function reservationSearchReply(int $http, bool $success, array $items, string $message = ''): never
{
    http_response_code($http);
    $payload = ['success' => $success, 'items' => $items];
    if ($message !== '') $payload['message'] = $message;
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

// GET continua aceito apenas para APKs antigos. A versao atual envia
// autenticacao e numero da reserva no corpo POST, evitando token no log da URL.
$method = $_SERVER['REQUEST_METHOD'] ?? '';
if (!in_array($method, ['GET', 'POST'], true)) {
    header('Allow: GET, POST');
    reservationSearchReply(405, false, [], 'Metodo nao permitido.');
}

$input = $_GET;
if ($method === 'POST') {
    $contentType = strtolower(trim(explode(';', (string) ($_SERVER['CONTENT_TYPE'] ?? ''))[0]));
    if ($contentType === 'application/json') {
        $raw = file_get_contents('php://input', false, null, 0, 8193);
        if ($raw === false || strlen($raw) > 8192) {
            reservationSearchReply(413, false, [], 'Requisicao muito grande.');
        }
        $input = json_decode($raw, true);
        if (!is_array($input) || array_is_list($input)) {
            reservationSearchReply(400, false, [], 'JSON invalido.');
        }
    } else {
        $input = $_POST;
    }
}

$term = trim((string) ($input['term'] ?? ''));
$exact = (string) ($input['exact'] ?? '') === '1';
$apiToken = trim((string) ($input['api_token'] ?? ''));

if (mb_strlen($term) < 3) {
    reservationSearchReply(200, true, []);
}
if (mb_strlen($term) > 100) {
    reservationSearchReply(422, false, [], 'Termo de busca muito longo.');
}
if ($apiToken === '') {
    reservationSearchReply(401, false, [], 'Usuario nao autenticado.');
}

try {
    $stmt = $pdo->prepare('SELECT id FROM ' . TABLE_USERS . ' WHERE api_token = :token AND is_active = 1 LIMIT 1');
    $stmt->execute([':token' => $apiToken]);
    if (!$stmt->fetch()) {
        reservationSearchReply(401, false, [], 'Sessao do usuario invalida.');
    }

    $config = require __DIR__ . '/../config.php';
    $reservationConfig = $config['reservation'] ?? [];
    $provider = strtolower((string) ($reservationConfig['provider'] ?? 'expresso'));
    $suggestionsSource = strtolower((string) ($reservationConfig['suggestions_source'] ?? 'none'));

    if ($exact && $provider !== 'hotelaria_sql') {
        require_once __DIR__ . '/../lib/ExpressoReservationClient.php';
        $found = (new ExpressoReservationClient($config['expresso'] ?? []))->lookup($term);
        reservationSearchReply(200, true, $found === [] ? [] : [$found]);
    }

    // A API Expresso documentada oferece consulta por codigo completo, nao autocomplete.
    // Em instalacoes migradas, nao consultar automaticamente as tabelas antigas.
    if (!$exact && $suggestionsSource !== 'hotelaria_sql') {
        reservationSearchReply(200, true, [], 'Digite o numero completo da reserva para consultar.');
    }

    // Integracao legada e opcional para instalacoes que ainda possuem as tabelas.
    $db = $config['hotelaria_db'];
    $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=%s',
        $db['host'], (int) $db['port'], $db['database'], $db['charset']);
    $hotelariaPdo = new PDO($dsn, $db['username'], $db['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    $select = 'SELECT r.id AS reserva_id, r.codigo, r.status, r.data_checkin, r.data_checkout,
                      COUNT(DISTINCT h.id) AS guest_count,
                      GROUP_CONCAT(DISTINCT h.nome ORDER BY h.nome SEPARATOR ", ") AS hospedes
               FROM reservas r
               LEFT JOIN reserva_hospedes rh ON rh.reserva_id = r.id AND rh.cliente_id = r.cliente_id
               LEFT JOIN hospedes h ON h.id = rh.hospede_id';
    if ($exact) {
        $stmt = $hotelariaPdo->prepare(
            $select . ' WHERE r.codigo = :term AND r.status IN ("confirmada", "checkin")
             GROUP BY r.id, r.codigo, r.status, r.data_checkin, r.data_checkout LIMIT 1'
        );
        $stmt->execute([':term' => $term]);
    } else {
        $stmt = $hotelariaPdo->prepare(
            $select . ' WHERE r.data_checkin = CURDATE() AND r.status IN ("confirmada", "checkin")
                  AND (r.codigo LIKE :code OR COALESCE(h.nome, "") LIKE :guest)
             GROUP BY r.id, r.codigo, r.status, r.data_checkin, r.data_checkout
             ORDER BY CASE WHEN r.status = "checkin" THEN 0 ELSE 1 END, r.codigo LIMIT 25'
        );
        $stmt->execute([':code' => '%' . $term . '%', ':guest' => '%' . $term . '%']);
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
            'source' => 'hotelaria_sql',
        ];
    }, $stmt->fetchAll());
    reservationSearchReply(200, true, $items);
} catch (Throwable $error) {
    error_log('[iPlate] Reservation search error: ' . $error->getMessage());
    reservationSearchReply(503, false, [], 'Consulta de reservas indisponivel. Verifique a integracao configurada.');
}
