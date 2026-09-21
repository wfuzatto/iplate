<?php

declare(strict_types=1);

$activePage = $activePage ?? 'entries';
$loggedUser = $loggedUser ?? null;
?>
<body>
    <nav class="navbar navbar-hub px-4 py-3">
        <div class="d-flex align-items-center gap-3">
            <div class="hub-brand-mark">IP</div>
            <div>
                <div class="fw-semibold">iPlate</div>
                <div class="small text-muted">Controle de entrada de veículos</div>
            </div>
        </div>
        <div class="d-flex align-items-center gap-3">
            <span class="text-muted small">
                <?= htmlspecialchars((string) ($loggedUser['name'] ?? 'Usuario'), ENT_QUOTES, 'UTF-8') ?>
                <?php if (!empty($loggedUser['username'])): ?>
                    <span class="d-block">@<?= htmlspecialchars((string) $loggedUser['username'], ENT_QUOTES, 'UTF-8') ?></span>
                <?php endif; ?>
            </span>
            <a class="btn btn-outline-secondary btn-sm" href="logout.php">Sair</a>
        </div>
    </nav>

    <div class="container-fluid">
        <div class="row">
            <aside class="col-lg-2 col-md-3 sidebar p-3">
                <div class="nav flex-column gap-2">
                    <a class="nav-link <?= $activePage === 'entries' ? 'active' : '' ?>" href="index.php">Entradas</a>
                    <a class="nav-link <?= $activePage === 'users' ? 'active' : '' ?>" href="users.php">Usuarios</a>
                </div>
            </aside>
            <main class="col-lg-10 col-md-9 p-4">
