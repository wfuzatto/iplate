<?php
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }

// Integration exercise of the real HTTP endpoint without DB or Expresso credentials.
$root = sys_get_temp_dir() . '/iplate_http_' . bin2hex(random_bytes(6));
mkdir($root . '/api', 0700, true);
mkdir($root . '/lib', 0700, true);
copy(dirname(__DIR__) . '/api/reservation-search.php', $root . '/api/reservation-search.php');
file_put_contents($root . '/db.php', <<<'PHP'
<?php
const TABLE_USERS = 'iplate_users';
class FixtureStatement {
    private bool $ok = false;
    public function execute(array $values): void { $this->ok = ($values[':token'] ?? '') === 'fixture-token'; }
    public function fetch(): array|false { return $this->ok ? ['id' => 1] : false; }
}
class FixtureConnection {
    public function prepare(string $query): FixtureStatement { return new FixtureStatement(); }
}
$pdo = new FixtureConnection();
PHP);
file_put_contents($root . '/config.php', <<<'PHP'
<?php
return ['reservation' => ['provider' => 'expresso', 'suggestions_source' => 'none'], 'expresso' => []];
PHP);
file_put_contents($root . '/lib/ExpressoReservationClient.php', <<<'PHP'
<?php
class ExpressoReservationClient {
    public function __construct(array $config) {}
    public function lookup(string $code): array {
        return $code === '2505371' ? [
            'reservation_id' => 'ABC-123',
            'reservation_code' => '2505371',
            'guest_name' => 'Pessoa fixture',
            'guest_cpf' => '12345678901',
        ] : [];
    }
}
PHP);
function assertHttp(bool $condition, string $message): void
{
    if (!$condition) throw new RuntimeException($message);
}
function doRequest(string $base, string $method, array|string $payload): array
{
    $body = is_array($payload) ? json_encode($payload, JSON_THROW_ON_ERROR) : $payload;
    $ctx = stream_context_create(['http' => [
        'method' => $method, 'header' => "Content-Type: application/json\r\n",
        'content' => $body, 'ignore_errors' => true, 'timeout' => 5,
    ]]);
    $raw = file_get_contents($base . '/api/reservation-search.php', false, $ctx);
    $headers = $http_response_header ?? [];
    preg_match('/HTTP\\/\\S+ (\\d{3})/', (string) ($headers[0] ?? ''), $m);
    return ['http' => (int) ($m[1] ?? 0), 'data' => is_string($raw) ? json_decode($raw, true) : null];
}
$sock = stream_socket_server('tcp://127.0.0.1:0', $errno, $error);
if (!$sock) throw new RuntimeException('Cannot obtain local HTTP port.');
$address = stream_socket_get_name($sock, false);
fclose($sock);
$port = (int) substr(strrchr($address, ':'), 1);
$base = 'http://127.0.0.1:' . $port;
$proc = proc_open([PHP_BINARY, '-S', '127.0.0.1:' . $port, '-t', $root],
    [0 => ['pipe', 'r'], 1 => ['pipe', 'w'], 2 => ['pipe', 'w']], $pipes);
if (!is_resource($proc)) throw new RuntimeException('Cannot start fixture HTTP server.');
fclose($pipes[0]);
try {
    $ready = false;
    for ($try = 0; $try < 40; $try++) {
        usleep(100000);
        $probe = @file_get_contents($base . '/config.php');
        if ($probe !== false) { $ready = true; break; }
    }
    assertHttp($ready, 'Test server did not start.');
    $valid = ['api_token' => 'fixture-token', 'term' => '2505371', 'exact' => '1'];
    $r = doRequest($base, 'POST', $valid);
    assertHttp($r['http'] === 200 && ($r['data']['success'] ?? false), 'POST should be valid');
    assertHttp(($r['data']['items'][0]['reservation_id'] ?? '') === 'ABC-123', 'POST must preserve reservation ID');
    assertHttp(($r['data']['items'][0]['guest_cpf'] ?? '') === '12345678901', 'POST must include guest CPF');
    $r = doRequest($base, 'POST', array_merge($valid, ['api_token' => 'wrong']));
    assertHttp($r['http'] === 401, 'Invalid session must return 401');
    $r = doRequest($base, 'POST', '{');
    assertHttp($r['http'] === 400, 'Malformed JSON must return 400');
    $r = doRequest($base, 'POST', array_merge($valid, ['exact' => '0']));
    assertHttp($r['http'] === 200 && $r['data']['items'] === [], 'No name autocomplete for Expresso');
    $r = doRequest($base, 'POST', array_merge($valid, ['term' => '9999999']));
    assertHttp($r['http'] === 200 && $r['data']['items'] === [], 'Unknown code must not be matched');
    $r = doRequest($base, 'PUT', $valid);
    assertHttp($r['http'] === 405, 'Unsupported method must return 405');
    echo "PASS: 8 HTTP endpoint assertions with fake DB/Expresso.\n";
} finally {
    proc_terminate($proc);
    fclose($pipes[1]); fclose($pipes[2]);
    proc_close($proc);
    foreach (['api/reservation-search.php','lib/ExpressoReservationClient.php','config.php','db.php'] as $p) unlink($root . '/' . $p);
    rmdir($root . '/api'); rmdir($root . '/lib'); rmdir($root);
}
