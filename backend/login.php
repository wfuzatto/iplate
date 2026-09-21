<?php

declare(strict_types=1);

require __DIR__ . '/db.php';
require __DIR__ . '/auth.php';
$pageTitle = 'iPlate | Acesso';

if (currentUser() !== null) {
    header('Location: index.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim((string) ($_POST['username'] ?? ''));
    $password = (string) ($_POST['password'] ?? '');

    $stmt = $pdo->prepare('SELECT id, name, username, password_hash FROM ' . TABLE_USERS . ' WHERE username = :username AND is_active = 1 LIMIT 1');
    $stmt->execute([':username' => $username]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, (string) $user['password_hash'])) {
        storeUserSession($user);
        header('Location: index.php');
        exit;
    }

    $error = 'Usuario ou senha invalidos.';
}
?>
<?php require __DIR__ . '/partials/head.php'; ?>
<body class="bg-light">
    <main class="container py-5">
        <div class="row justify-content-center">
            <div class="col-md-6 col-lg-4">
                <div class="card shadow-sm">
                    <div class="card-body p-4">
                        <div class="text-center mb-4">
                            <div class="hub-brand-mark mx-auto mb-3">IP</div>
                            <h4 class="fw-bold mb-1">Acesso ao iPlate</h4>
                            <p class="text-muted mb-0">Modulo de controle de entrada de veiculos</p>
                        </div>

                        <form method="post">
                            <div class="mb-3">
                                <label class="form-label">Usuario</label>
                                <input type="text" name="username" class="form-control" required autofocus>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Senha</label>
                                <input type="password" name="password" class="form-control" required>
                            </div>
                            <?php if ($error !== ''): ?>
                                <div class="alert alert-danger"><?= htmlspecialchars($error) ?></div>
                            <?php endif; ?>
                            <button class="btn btn-hub w-100">Entrar</button>
                        </form>

                    </div>
                </div>
            </div>
        </div>
    </main>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
