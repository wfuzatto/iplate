<?php

declare(strict_types=1);

require __DIR__ . '/db.php';
require __DIR__ . '/auth.php';
$config = require __DIR__ . '/config.php';
requireLogin();

$loggedUser = currentUser();
$pageTitle = $config['app']['name'] . ' | Entradas';
$activePage = 'entries';

$search = trim((string) ($_GET['search'] ?? ''));
$dateFrom = trim((string) ($_GET['date_from'] ?? ''));
$dateTo = trim((string) ($_GET['date_to'] ?? ''));

$where = [];
$params = [];

if ($search !== '') {
    $where[] = '(plate LIKE :search_plate OR reservation LIKE :search_reservation OR brand LIKE :search_brand OR model LIKE :search_model OR operator_name LIKE :search_operator OR ' . TABLE_USERS . '.username LIKE :search_username)';
    $searchParam = '%' . $search . '%';
    $params[':search_plate'] = $searchParam;
    $params[':search_reservation'] = $searchParam;
    $params[':search_brand'] = $searchParam;
    $params[':search_model'] = $searchParam;
    $params[':search_operator'] = $searchParam;
    $params[':search_username'] = $searchParam;
}

if ($dateFrom !== '') {
    $where[] = 'DATE(' . TABLE_VEHICLE_ENTRIES . '.created_at) >= :date_from';
    $params[':date_from'] = $dateFrom;
}

if ($dateTo !== '') {
    $where[] = 'DATE(' . TABLE_VEHICLE_ENTRIES . '.created_at) <= :date_to';
    $params[':date_to'] = $dateTo;
}

$sql = 'SELECT
    ' . TABLE_VEHICLE_ENTRIES . '.*,
    ' . TABLE_USERS . '.username,
    ' . TABLE_VEHICLE_ENTRIES . '.front_photo_path AS dashboard_front_photo_path
FROM ' . TABLE_VEHICLE_ENTRIES . '
LEFT JOIN ' . TABLE_USERS . ' ON ' . TABLE_USERS . '.id = ' . TABLE_VEHICLE_ENTRIES . '.user_id';
if ($where !== []) {
    $sql .= ' WHERE ' . implode(' AND ', $where);
}
$sql .= ' ORDER BY ' . TABLE_VEHICLE_ENTRIES . '.created_at DESC LIMIT 300';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$entries = $stmt->fetchAll();

$stats = [
    'today' => (int) $pdo->query('SELECT COUNT(*) FROM ' . TABLE_VEHICLE_ENTRIES . ' WHERE DATE(created_at) = CURDATE()')->fetchColumn(),
    'total' => (int) $pdo->query('SELECT COUNT(*) FROM ' . TABLE_VEHICLE_ENTRIES)->fetchColumn(),
    'with_photo' => (int) $pdo->query('SELECT COUNT(*) FROM ' . TABLE_VEHICLE_ENTRIES . ' WHERE plate_photo_path IS NOT NULL OR front_photo_path IS NOT NULL OR driver_side_photo_path IS NOT NULL OR rear_photo_path IS NOT NULL OR passenger_side_photo_path IS NOT NULL')->fetchColumn(),
];

function uploadUrl(string $baseUrl, ?string $relativePath): string
{
    if ($relativePath === null || $relativePath === '') {
        return '';
    }

    $effectiveBaseUrl = trim($baseUrl);
    if ($effectiveBaseUrl === '') {
        $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        $host = (string) ($_SERVER['HTTP_HOST'] ?? 'localhost');
        $scriptDir = rtrim(str_replace('\\', '/', dirname((string) ($_SERVER['SCRIPT_NAME'] ?? '/'))), '/');
        $effectiveBaseUrl = $scheme . '://' . $host . ($scriptDir === '' ? '' : $scriptDir);
    }

    return rtrim($effectiveBaseUrl, '/') . '/' . ltrim($relativePath, '/');
}
?>
<?php require __DIR__ . '/partials/head.php'; ?>
<?php require __DIR__ . '/partials/shell_start.php'; ?>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="fw-bold">Entradas de Veiculos</h2>
        <p class="text-muted mb-0">Consulta dos registros enviados pelo app iPlate.</p>
    </div>
</div>

<div class="row g-3 mb-4 row-cols-1 row-cols-sm-2 row-cols-xl-3">
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Entradas hoje</span>
                <h3 class="fw-bold mb-0"><?= $stats['today'] ?></h3>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Total cadastrado</span>
                <h3 class="fw-bold mb-0"><?= $stats['total'] ?></h3>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Com foto</span>
                <h3 class="fw-bold mb-0"><?= $stats['with_photo'] ?></h3>
            </div>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-body">
        <form method="get" class="row g-3 mb-4">
            <div class="col-lg-5">
                <label class="form-label">Busca</label>
                <input type="text" class="form-control" name="search" placeholder="Placa, reserva, marca ou modelo" value="<?= htmlspecialchars($search) ?>">
            </div>
            <div class="col-md-3 col-lg-2">
                <label class="form-label">De</label>
                <input type="date" class="form-control" name="date_from" value="<?= htmlspecialchars($dateFrom) ?>">
            </div>
            <div class="col-md-3 col-lg-2">
                <label class="form-label">Ate</label>
                <input type="date" class="form-control" name="date_to" value="<?= htmlspecialchars($dateTo) ?>">
            </div>
            <div class="col-md-6 col-lg-3 d-flex align-items-end gap-2">
                <button type="submit" class="btn btn-hub">Filtrar</button>
                <a href="index.php" class="btn btn-outline-secondary">Limpar</a>
            </div>
        </form>

        <div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead>
                    <tr>
                        <th>Data/Hora</th>
                        <th>Placa</th>
                        <th>Reserva</th>
                        <th>Marca</th>
                        <th>Modelo</th>
                        <th>Operador</th>
                        <th>Fotos</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if ($entries === []): ?>
                        <tr>
                            <td colspan="7" class="text-center text-muted py-4">Nenhum registro encontrado.</td>
                        </tr>
                    <?php endif; ?>

                    <?php foreach ($entries as $entry): ?>
                        <tr>
                            <td>
                                <div class="fw-semibold"><?= htmlspecialchars(date('d/m/Y H:i:s', strtotime((string) $entry['created_at']))) ?></div>
                                <?php if (!empty($entry['client_timestamp'])): ?>
                                    <small class="text-muted">App: <?= htmlspecialchars((string) $entry['client_timestamp']) ?></small>
                                <?php endif; ?>
                            </td>
                            <td><?= htmlspecialchars((string) $entry['plate']) ?></td>
                            <td><?= htmlspecialchars((string) $entry['reservation']) ?></td>
                            <td><?= htmlspecialchars((string) $entry['brand']) ?></td>
                            <td><?= htmlspecialchars((string) $entry['model']) ?></td>
                            <td>
                                <div class="fw-semibold"><?= htmlspecialchars((string) ($entry['operator_name'] ?? '')) ?></div>
                                <?php if (!empty($entry['username'])): ?>
                                    <small class="text-muted">@<?= htmlspecialchars((string) $entry['username']) ?></small>
                                <?php endif; ?>
                            </td>
                            <td>
                                <div class="d-flex flex-wrap gap-2">
                                    <?php if (!empty($entry['plate_photo_path'])): ?>
                                        <?php $plateUrl = uploadUrl($config['app']['base_url'], (string) $entry['plate_photo_path']); ?>
                                        <button
                                            type="button"
                                            class="btn btn-sm btn-outline-secondary js-photo-modal"
                                            data-bs-toggle="modal"
                                            data-bs-target="#photoModal"
                                            data-photo-url="<?= htmlspecialchars($plateUrl) ?>"
                                            data-photo-title="Foto da placa"
                                        >
                                            Placa
                                        </button>
                                    <?php endif; ?>
                                    <?php if (!empty($entry['dashboard_front_photo_path'])): ?>
                                        <?php $frontUrl = uploadUrl($config['app']['base_url'], (string) $entry['dashboard_front_photo_path']); ?>
                                        <button
                                            type="button"
                                            class="btn btn-sm btn-outline-secondary js-photo-modal"
                                            data-bs-toggle="modal"
                                            data-bs-target="#photoModal"
                                            data-photo-url="<?= htmlspecialchars($frontUrl) ?>"
                                            data-photo-title="Foto da frente"
                                        >
                                            Frente
                                        </button>
                                    <?php endif; ?>
                                    <?php if (!empty($entry['driver_side_photo_path'])): ?>
                                        <?php $driverUrl = uploadUrl($config['app']['base_url'], (string) $entry['driver_side_photo_path']); ?>
                                        <button
                                            type="button"
                                            class="btn btn-sm btn-outline-secondary js-photo-modal"
                                            data-bs-toggle="modal"
                                            data-bs-target="#photoModal"
                                            data-photo-url="<?= htmlspecialchars($driverUrl) ?>"
                                            data-photo-title="Foto lateral do motorista"
                                        >
                                            Motorista
                                        </button>
                                    <?php endif; ?>
                                    <?php if (!empty($entry['rear_photo_path'])): ?>
                                        <?php $rearUrl = uploadUrl($config['app']['base_url'], (string) $entry['rear_photo_path']); ?>
                                        <button
                                            type="button"
                                            class="btn btn-sm btn-outline-secondary js-photo-modal"
                                            data-bs-toggle="modal"
                                            data-bs-target="#photoModal"
                                            data-photo-url="<?= htmlspecialchars($rearUrl) ?>"
                                            data-photo-title="Foto traseira"
                                        >
                                            Traseira
                                        </button>
                                    <?php endif; ?>
                                    <?php if (!empty($entry['passenger_side_photo_path'])): ?>
                                        <?php $passengerUrl = uploadUrl($config['app']['base_url'], (string) $entry['passenger_side_photo_path']); ?>
                                        <button
                                            type="button"
                                            class="btn btn-sm btn-outline-secondary js-photo-modal"
                                            data-bs-toggle="modal"
                                            data-bs-target="#photoModal"
                                            data-photo-url="<?= htmlspecialchars($passengerUrl) ?>"
                                            data-photo-title="Foto do lado do carona"
                                        >
                                            Carona
                                        </button>
                                    <?php endif; ?>
                                    <?php if (
                                        empty($entry['plate_photo_path']) &&
                                        empty($entry['dashboard_front_photo_path']) &&
                                        empty($entry['driver_side_photo_path']) &&
                                        empty($entry['rear_photo_path']) &&
                                        empty($entry['passenger_side_photo_path'])
                                    ): ?>
                                        <span class="text-muted">Sem foto</span>
                                    <?php endif; ?>
                                </div>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div class="modal fade" id="photoModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-xl photo-modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="photoModalTitle">Visualizacao da foto</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>
            <div class="modal-body text-center photo-modal-body">
                <a id="photoModalLink" href="" target="_blank" rel="noopener">
                    <img id="photoModalImage" src="" alt="" class="img-fluid rounded border photo-modal-image">
                </a>
            </div>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function () {
    const modal = document.getElementById('photoModal');
    const modalTitle = document.getElementById('photoModalTitle');
    const modalImage = document.getElementById('photoModalImage');
    const modalLink = document.getElementById('photoModalLink');

    document.querySelectorAll('.js-photo-modal').forEach(function (button) {
        button.addEventListener('click', function () {
            const photoUrl = button.dataset.photoUrl || '';
            modalTitle.textContent = button.dataset.photoTitle || 'Visualizacao da foto';
            modalImage.src = photoUrl;
            modalImage.alt = button.dataset.photoTitle || 'Foto';
            modalLink.href = photoUrl;
        });
    });

    modal.addEventListener('hidden.bs.modal', function () {
        modalImage.src = '';
        modalImage.alt = '';
        modalLink.href = '';
    });
});
</script>

<?php require __DIR__ . '/partials/shell_end.php'; ?>
