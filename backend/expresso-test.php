<?php
declare(strict_types=1);

date_default_timezone_set('America/Sao_Paulo');

$defaultTokenUrl = 'https://vale.expresso.app/api/obter_token';
$defaultReservaUrl = 'https://vale.expresso.app/api/reserva';
$defaultAnexoUrl = 'https://vale.expresso.app/api/anexo';
$defaultUser = getenv('IPLATE_EXPRESSO_USER') ?: '';
$defaultPassword = getenv('IPLATE_EXPRESSO_PASSWORD') ?: '';
$defaultReserva = '123123';

function postJson(string $url, array $payload): array
{
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
        CURLOPT_POSTFIELDS => json_encode($payload, JSON_UNESCAPED_UNICODE),
        CURLOPT_TIMEOUT => 30,
    ]);
    $raw = curl_exec($ch);
    $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    return [
        'http_code' => $code,
        'raw' => $raw === false ? '' : $raw,
        'error' => $error,
        'json' => json_decode((string) $raw, true),
    ];
}

function postMultipart(string $url, array $fields): array
{
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $fields,
        CURLOPT_TIMEOUT => 60,
    ]);
    $raw = curl_exec($ch);
    $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    return [
        'http_code' => $code,
        'raw' => $raw === false ? '' : $raw,
        'error' => $error,
        'json' => json_decode((string) $raw, true),
    ];
}

function findReservaId(?array $json): string
{
    if (!$json) {
        return '';
    }

    $sources = [];
    if (isset($json['data']) && is_array($json['data'])) {
        $sources[] = $json['data'];
    }
    if (isset($json['reserva']) && is_array($json['reserva'])) {
        $sources[] = $json['reserva'];
    }
    $sources[] = $json;

    $keys = ['reserva_id', 'reservation_id', 'id'];
    foreach ($sources as $source) {
        foreach ($keys as $key) {
            $value = $source[$key] ?? '';
            if ($value !== null && (string) $value !== '') {
                return (string) $value;
            }
        }
    }

    return '';
}

$result = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $tokenUrl = trim((string) ($_POST['token_url'] ?? $defaultTokenUrl));
    $reservaUrl = trim((string) ($_POST['reserva_url'] ?? $defaultReservaUrl));
    $anexoUrl = trim((string) ($_POST['anexo_url'] ?? $defaultAnexoUrl));
    $user = trim((string) ($_POST['user'] ?? $defaultUser));
    $password = (string) ($_POST['password'] ?? $defaultPassword);
    $numeroReserva = trim((string) ($_POST['numero_reserva'] ?? $defaultReserva));
    $detalhes = trim((string) ($_POST['detalhes'] ?? 'Texto de teste iPlate'));
    $tag = trim((string) ($_POST['tag'] ?? '475'));

    $result = [
        'steps' => [],
        'ok' => false,
    ];

    if (!isset($_FILES['test_file']) || !is_uploaded_file($_FILES['test_file']['tmp_name'])) {
        $result['steps'][] = ['name' => 'Validacao', 'ok' => false, 'message' => 'Selecione uma foto de teste.'];
    } else {
        $tokenResponse = postJson($tokenUrl, [
            'user' => $user,
            'password' => $password,
        ]);
        $token = (string) ($tokenResponse['json']['token'] ?? '');
        $result['steps'][] = [
            'name' => 'Obter token',
            'ok' => $token !== '' && $tokenResponse['error'] === '',
            'http_code' => $tokenResponse['http_code'],
            'error' => $tokenResponse['error'],
            'raw' => $tokenResponse['raw'],
        ];

        if ($token !== '') {
            $reservaResponse = postJson($reservaUrl, [
                'token' => $token,
                'numero_reserva' => $numeroReserva,
            ]);
            $reservaId = findReservaId($reservaResponse['json']);
            $result['steps'][] = [
                'name' => 'Consultar reserva',
                'ok' => $reservaId !== '' && $reservaResponse['error'] === '',
                'http_code' => $reservaResponse['http_code'],
                'error' => $reservaResponse['error'],
                'raw' => $reservaResponse['raw'],
                'reserva_id_detectado' => $reservaId,
            ];

            if ($reservaId !== '') {
                $tmpName = (string) $_FILES['test_file']['tmp_name'];
                $originalName = (string) ($_FILES['test_file']['name'] ?? 'teste.jpg');
                $mimeType = (string) ($_FILES['test_file']['type'] ?? 'image/jpeg');
                $filePart = curl_file_create($tmpName, $mimeType, $originalName);

                $anexoResponse = postMultipart($anexoUrl, [
                    'token' => $token,
                    'ref_table' => 'reservas',
                    'ref_id' => $reservaId,
                    'detalhes' => $detalhes,
                    'tags[]' => $tag,
                    'file' => $filePart,
                ]);

                $okAnexo = $anexoResponse['error'] === '' && $anexoResponse['http_code'] >= 200 && $anexoResponse['http_code'] < 300;
                $result['steps'][] = [
                    'name' => 'Enviar anexo (foto + texto)',
                    'ok' => $okAnexo,
                    'http_code' => $anexoResponse['http_code'],
                    'error' => $anexoResponse['error'],
                    'raw' => $anexoResponse['raw'],
                ];
                $result['ok'] = $okAnexo;
            }
        }
    }
}
?>
<!doctype html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>iPlate - Teste Expresso</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
    <h1 class="h4 mb-3">Teste auxiliar - Expresso</h1>
    <p class="text-muted">Fluxo: token -> reserva -> anexo (foto + texto) com <code>tags[]</code>.</p>

    <form method="post" enctype="multipart/form-data" class="card card-body mb-4">
        <div class="row g-3">
            <div class="col-md-4">
                <label class="form-label">Token URL</label>
                <input class="form-control" name="token_url" value="<?= htmlspecialchars($_POST['token_url'] ?? $defaultTokenUrl, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Reserva URL</label>
                <input class="form-control" name="reserva_url" value="<?= htmlspecialchars($_POST['reserva_url'] ?? $defaultReservaUrl, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Anexo URL</label>
                <input class="form-control" name="anexo_url" value="<?= htmlspecialchars($_POST['anexo_url'] ?? $defaultAnexoUrl, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Usuário</label>
                <input class="form-control" name="user" value="<?= htmlspecialchars($_POST['user'] ?? $defaultUser, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Senha</label>
                <input type="password" class="form-control" name="password" value="<?= htmlspecialchars($_POST['password'] ?? $defaultPassword, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Reserva teste</label>
                <input class="form-control" name="numero_reserva" value="<?= htmlspecialchars($_POST['numero_reserva'] ?? $defaultReserva, ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-6">
                <label class="form-label">Texto de teste (detalhes)</label>
                <input class="form-control" name="detalhes" value="<?= htmlspecialchars($_POST['detalhes'] ?? 'Texto de teste iPlate', ENT_QUOTES) ?>">
            </div>
            <div class="col-md-2">
                <label class="form-label">Tag</label>
                <input class="form-control" name="tag" value="<?= htmlspecialchars($_POST['tag'] ?? '475', ENT_QUOTES) ?>" required>
            </div>
            <div class="col-md-4">
                <label class="form-label">Foto de teste</label>
                <input type="file" class="form-control" name="test_file" accept="image/*" required>
            </div>
        </div>
        <div class="mt-3">
            <button class="btn btn-primary" type="submit">Executar teste</button>
        </div>
    </form>

    <?php if ($result !== null): ?>
        <div class="alert <?= $result['ok'] ? 'alert-success' : 'alert-warning' ?>">
            Resultado geral: <strong><?= $result['ok'] ? 'SUCESSO' : 'FALHA' ?></strong>
        </div>

        <?php foreach ($result['steps'] as $step): ?>
            <div class="card mb-3">
                <div class="card-header">
                    <?= htmlspecialchars((string) $step['name']) ?> -
                    <strong class="<?= !empty($step['ok']) ? 'text-success' : 'text-danger' ?>">
                        <?= !empty($step['ok']) ? 'OK' : 'ERRO' ?>
                    </strong>
                </div>
                <div class="card-body">
                    <?php if (isset($step['http_code'])): ?>
                        <p class="mb-1"><strong>HTTP:</strong> <?= (int) $step['http_code'] ?></p>
                    <?php endif; ?>
                    <?php if (!empty($step['reserva_id_detectado'])): ?>
                        <p class="mb-1"><strong>reserva_id:</strong> <?= htmlspecialchars((string) $step['reserva_id_detectado']) ?></p>
                    <?php endif; ?>
                    <?php if (!empty($step['error'])): ?>
                        <p class="mb-1 text-danger"><strong>cURL erro:</strong> <?= htmlspecialchars((string) $step['error']) ?></p>
                    <?php endif; ?>
                    <?php if (isset($step['raw'])): ?>
                        <pre class="mb-0 bg-light p-2 border rounded small"><?= htmlspecialchars((string) $step['raw']) ?></pre>
                    <?php endif; ?>
                </div>
            </div>
        <?php endforeach; ?>
    <?php endif; ?>
</div>
</body>
</html>
