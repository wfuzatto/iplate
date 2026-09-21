<?php

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');

require __DIR__ . '/../db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Metodo nao permitido.']);
    exit;
}

$username = trim((string) ($_POST['username'] ?? ''));
$password = (string) ($_POST['password'] ?? '');

if ($username === '' || $password === '') {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => 'Informe usuario e senha.']);
    exit;
}

$stmt = $pdo->prepare('SELECT id, name, username, password_hash, api_token FROM ' . TABLE_USERS . ' WHERE username = :username AND is_active = 1 LIMIT 1');
$stmt->execute([':username' => $username]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, (string) $user['password_hash'])) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Usuario ou senha invalidos.']);
    exit;
}

echo json_encode([
    'success' => true,
    'message' => 'Login realizado com sucesso.',
    'user' => [
        'id' => (int) $user['id'],
        'name' => (string) $user['name'],
        'username' => (string) $user['username'],
        'api_token' => (string) $user['api_token'],
    ],
]);
