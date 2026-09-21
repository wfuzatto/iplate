<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

require __DIR__ . '/../db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Metodo nao permitido.']);
    exit;
}

$plate = strtoupper(trim((string) ($_POST['plate'] ?? '')));
$reservation = trim((string) ($_POST['reservation'] ?? ''));
$brand = trim((string) ($_POST['brand'] ?? ''));
$model = trim((string) ($_POST['model'] ?? ''));
$clientTimestamp = trim((string) ($_POST['client_timestamp'] ?? ''));
$apiToken = trim((string) ($_POST['api_token'] ?? ''));

$errors = [];

if ($plate === '') {
    $errors[] = 'Informe a placa.';
}

if ($reservation === '') {
    $errors[] = 'Informe a reserva.';
}

if ($brand === '') {
    $errors[] = 'Informe a marca.';
}

if ($model === '') {
    $errors[] = 'Informe o modelo.';
}

if ($apiToken === '') {
    $errors[] = 'Usuario nao autenticado.';
}

foreach (['plate_photo', 'front_photo', 'driver_side_photo', 'rear_photo', 'passenger_side_photo'] as $requiredPhoto) {
    if (empty($_FILES[$requiredPhoto]['tmp_name'])) {
        $errors[] = 'Envie as 5 fotos obrigatorias: placa, frente, lateral do motorista, traseira e lado do carona.';
        break;
    }
}

if ($errors !== []) {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => implode(' ', $errors)]);
    exit;
}

/**
 * @return array{full:string, relative:string}|null
 */
function storeUpload(array $file, string $label): ?array
{
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE || empty($file['tmp_name'])) {
        return null;
    }

    if (($file['error'] ?? UPLOAD_ERR_OK) !== UPLOAD_ERR_OK) {
        throw new RuntimeException('Falha no upload da foto: ' . $label);
    }

    $allowedMimeTypes = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp',
    ];

    $mimeType = mime_content_type($file['tmp_name']);
    if (!isset($allowedMimeTypes[$mimeType])) {
        throw new RuntimeException('Formato de imagem invalido para: ' . $label);
    }

    $relativeDir = '/uploads/' . date('Y/m');
    $targetDir = dirname(__DIR__) . $relativeDir;

    $baseUploadDir = dirname(__DIR__) . '/uploads';
    if (!is_dir($baseUploadDir)) {
        throw new RuntimeException('Pasta base de uploads nao encontrada: ' . $baseUploadDir);
    }

    if (!is_writable($baseUploadDir)) {
        throw new RuntimeException('Pasta base de uploads sem permissao de escrita: ' . $baseUploadDir);
    }

    if (!is_dir($targetDir) && !mkdir($targetDir, 0775, true) && !is_dir($targetDir)) {
        throw new RuntimeException('Nao foi possivel criar a pasta de uploads em: ' . $targetDir);
    }

    $fileName = sprintf(
        '%s_%s_%s.%s',
        $label,
        date('Ymd_His'),
        bin2hex(random_bytes(4)),
        $allowedMimeTypes[$mimeType]
    );

    $targetFile = $targetDir . '/' . $fileName;
    if (!move_uploaded_file($file['tmp_name'], $targetFile)) {
        throw new RuntimeException('Nao foi possivel salvar a foto: ' . $label);
    }

    return [
        'full' => $targetFile,
        'relative' => $relativeDir . '/' . $fileName,
    ];
}

try {
    $userStmt = $pdo->prepare('SELECT id, name FROM ' . TABLE_USERS . ' WHERE api_token = :api_token AND is_active = 1 LIMIT 1');
    $userStmt->execute([':api_token' => $apiToken]);
    $user = $userStmt->fetch();

    if (!$user) {
        http_response_code(401);
        echo json_encode(['success' => false, 'message' => 'Sessao do usuario invalida.']);
        exit;
    }

    $platePhoto = storeUpload($_FILES['plate_photo'] ?? [], 'plate');
    $frontPhoto = storeUpload($_FILES['front_photo'] ?? [], 'front');
    $driverSidePhoto = storeUpload($_FILES['driver_side_photo'] ?? [], 'driver_side');
    $rearPhoto = storeUpload($_FILES['rear_photo'] ?? [], 'rear');
    $passengerSidePhoto = storeUpload($_FILES['passenger_side_photo'] ?? [], 'passenger_side');

    $stmt = $pdo->prepare(
        'INSERT INTO ' . TABLE_VEHICLE_ENTRIES . ' (
            user_id,
            operator_name,
            plate,
            reservation,
            brand,
            model,
            plate_photo_path,
            front_photo_path,
            driver_side_photo_path,
            rear_photo_path,
            passenger_side_photo_path,
            client_timestamp,
            created_at
        ) VALUES (
            :user_id,
            :operator_name,
            :plate,
            :reservation,
            :brand,
            :model,
            :plate_photo_path,
            :front_photo_path,
            :driver_side_photo_path,
            :rear_photo_path,
            :passenger_side_photo_path,
            :client_timestamp,
            NOW()
        )'
    );

    $stmt->execute([
        ':user_id' => (int) $user['id'],
        ':operator_name' => (string) $user['name'],
        ':plate' => $plate,
        ':reservation' => $reservation,
        ':brand' => $brand,
        ':model' => $model,
        ':plate_photo_path' => $platePhoto['relative'] ?? null,
        ':front_photo_path' => $frontPhoto['relative'] ?? null,
        ':driver_side_photo_path' => $driverSidePhoto['relative'] ?? null,
        ':rear_photo_path' => $rearPhoto['relative'] ?? null,
        ':passenger_side_photo_path' => $passengerSidePhoto['relative'] ?? null,
        ':client_timestamp' => $clientTimestamp !== '' ? $clientTimestamp : null,
    ]);

    echo json_encode([
        'success' => true,
        'message' => 'Entrada registrada com sucesso.',
        'entry_id' => (int) $pdo->lastInsertId(),
    ]);
} catch (Throwable $throwable) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => $throwable->getMessage(),
    ]);
}
