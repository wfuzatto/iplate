<?php

declare(strict_types=1);

require __DIR__ . '/db.php';
require __DIR__ . '/auth.php';
requireLogin();

$loggedUser = currentUser();
if (($loggedUser['username'] ?? '') !== 'admin') {
    http_response_code(403);
    exit('Acesso negado.');
}

$pageTitle = 'iPlate | Usuarios';
$activePage = 'users';
$error = '';
$success = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = (string) ($_POST['action'] ?? 'create');

    if ($action === 'toggle_status') {
        $userId = (int) ($_POST['user_id'] ?? 0);
        $activate = (int) ($_POST['activate'] ?? 0) === 1 ? 1 : 0;

        if ($userId <= 0) {
            $error = 'Usuario invalido.';
        } elseif ($loggedUser !== null && $userId === (int) $loggedUser['id'] && $activate === 0) {
            $error = 'Nao e permitido desativar o usuario logado.';
        } else {
            $stmt = $pdo->prepare('UPDATE ' . TABLE_USERS . ' SET is_active = :is_active WHERE id = :id');
            $stmt->execute([
                ':is_active' => $activate,
                ':id' => $userId,
            ]);
            $success = $activate === 1 ? 'Usuario ativado com sucesso.' : 'Usuario desativado com sucesso.';
        }
    }

    if ($action === 'create') {
        $name = trim((string) ($_POST['name'] ?? ''));
        $username = trim((string) ($_POST['username'] ?? ''));
        $password = (string) ($_POST['password'] ?? '');

        if ($name === '' || $username === '' || $password === '') {
            $error = 'Preencha nome, usuario e senha.';
        } else {
            $existsStmt = $pdo->prepare('SELECT id FROM ' . TABLE_USERS . ' WHERE username = :username LIMIT 1');
            $existsStmt->execute([':username' => $username]);
            $existingUser = $existsStmt->fetch();

            if ($existingUser) {
                $error = 'Ja existe um usuario com esse login.';
            } else {
                $insertStmt = $pdo->prepare(
                    'INSERT INTO ' . TABLE_USERS . ' (name, username, password_hash, api_token, is_active)
                     VALUES (:name, :username, :password_hash, :api_token, 1)'
                );
                $insertStmt->execute([
                    ':name' => $name,
                    ':username' => $username,
                    ':password_hash' => password_hash($password, PASSWORD_DEFAULT),
                    ':api_token' => bin2hex(random_bytes(32)),
                ]);
                $success = 'Usuario criado com sucesso.';
            }
        }
    }
}

$usersStmt = $pdo->query(
    'SELECT
        ' . TABLE_USERS . '.*,
        (SELECT COUNT(*) FROM ' . TABLE_VEHICLE_ENTRIES . ' WHERE ' . TABLE_VEHICLE_ENTRIES . '.user_id = ' . TABLE_USERS . '.id) AS entries_count
     FROM ' . TABLE_USERS . '
     ORDER BY ' . TABLE_USERS . '.created_at DESC'
);
$users = $usersStmt->fetchAll();
?>
<?php require __DIR__ . '/partials/head.php'; ?>
<?php require __DIR__ . '/partials/shell_start.php'; ?>

<div class="d-flex justify-content-between align-items-center mb-4">
    <div>
        <h2 class="fw-bold">Usuarios</h2>
        <p class="text-muted mb-0">Cadastre operadores para o app Android e acompanhe o acesso ao sistema.</p>
    </div>
</div>

<div class="row g-3 mb-4 row-cols-1 row-cols-sm-2 row-cols-xl-3">
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Total de usuarios</span>
                <h3 class="fw-bold mb-0"><?= count($users) ?></h3>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Usuarios ativos</span>
                <h3 class="fw-bold mb-0"><?= count(array_filter($users, static fn(array $user): bool => (int) $user['is_active'] === 1)) ?></h3>
            </div>
        </div>
    </div>
    <div class="col">
        <div class="card card-accent h-100">
            <div class="card-body">
                <span class="text-muted">Cadastros realizados</span>
                <h3 class="fw-bold mb-0"><?= array_sum(array_map(static fn(array $user): int => (int) $user['entries_count'], $users)) ?></h3>
            </div>
        </div>
    </div>
</div>

<div class="row g-4">
    <div class="col-xl-4">
        <div class="card">
            <div class="card-body">
                <h5 class="fw-bold">Novo usuario</h5>
                <p class="text-muted">O usuario criado aqui podera fazer login no app e no painel web.</p>

                <?php if ($error !== ''): ?>
                    <div class="alert alert-danger"><?= htmlspecialchars($error) ?></div>
                <?php endif; ?>

                <?php if ($success !== ''): ?>
                    <div class="alert alert-success"><?= htmlspecialchars($success) ?></div>
                <?php endif; ?>

                <form method="post">
                    <input type="hidden" name="action" value="create">
                    <div class="mb-3">
                        <label class="form-label">Nome completo</label>
                        <input type="text" class="form-control" name="name" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Usuario</label>
                        <input type="text" class="form-control" name="username" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Senha inicial</label>
                        <input type="password" class="form-control" name="password" required>
                    </div>
                    <button type="submit" class="btn btn-hub">Criar usuario</button>
                </form>
            </div>
        </div>
    </div>

    <div class="col-xl-8">
        <div class="card">
            <div class="card-body">
                <h5 class="fw-bold">Usuarios cadastrados</h5>
                <p class="text-muted">Ative ou desative operadores sem apagar o historico de entradas.</p>

                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Usuario</th>
                                <th>Status</th>
                                <th>Entradas</th>
                                <th>Criado em</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php foreach ($users as $user): ?>
                                <tr>
                                    <td>
                                        <div class="fw-semibold"><?= htmlspecialchars((string) $user['name']) ?></div>
                                        <?php if ((int) $user['id'] === (int) ($loggedUser['id'] ?? 0)): ?>
                                            <small class="text-muted">usuario atual</small>
                                        <?php endif; ?>
                                    </td>
                                    <td>@<?= htmlspecialchars((string) $user['username']) ?></td>
                                    <td>
                                        <span class="badge <?= (int) $user['is_active'] === 1 ? 'text-bg-success' : 'text-bg-secondary' ?>">
                                            <?= (int) $user['is_active'] === 1 ? 'Ativo' : 'Inativo' ?>
                                        </span>
                                    </td>
                                    <td><?= (int) $user['entries_count'] ?></td>
                                    <td><?= htmlspecialchars(date('d/m/Y H:i', strtotime((string) $user['created_at']))) ?></td>
                                    <td class="text-end">
                                        <form method="post" class="d-inline">
                                            <input type="hidden" name="action" value="toggle_status">
                                            <input type="hidden" name="user_id" value="<?= (int) $user['id'] ?>">
                                            <input type="hidden" name="activate" value="<?= (int) $user['is_active'] === 1 ? 0 : 1 ?>">
                                            <button
                                                type="submit"
                                                class="btn btn-sm <?= (int) $user['is_active'] === 1 ? 'btn-outline-danger' : 'btn-outline-secondary' ?>"
                                                <?= ((int) $user['id'] === (int) ($loggedUser['id'] ?? 0) && (int) $user['is_active'] === 1) ? 'disabled' : '' ?>
                                            >
                                                <?= (int) $user['is_active'] === 1 ? 'Desativar' : 'Ativar' ?>
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<?php require __DIR__ . '/partials/shell_end.php'; ?>
