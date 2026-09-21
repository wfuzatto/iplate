<?php

declare(strict_types=1);

if (session_status() !== PHP_SESSION_ACTIVE) {
    session_start();
}

/**
 * @return array{id:int,name:string,username:string}|null
 */
function currentUser(): ?array
{
    if (!isset($_SESSION['user']) || !is_array($_SESSION['user'])) {
        return null;
    }

    return $_SESSION['user'];
}

function requireLogin(): void
{
    if (currentUser() !== null) {
        return;
    }

    header('Location: login.php');
    exit;
}

function storeUserSession(array $user): void
{
    $_SESSION['user'] = [
        'id' => (int) $user['id'],
        'name' => (string) $user['name'],
        'username' => (string) $user['username'],
    ];
}
