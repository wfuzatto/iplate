<?php
declare(strict_types=1);

if (PHP_SAPI!=='cli') { http_response_code(404); exit; }

// Uses an isolated temporary database on local XAMPP, never backend/config.php.
$database='aqv_iplate_test_'.bin2hex(random_bytes(6));
$directory=sys_get_temp_dir().DIRECTORY_SEPARATOR.$database;
$password=(string)(getenv('AQV_TEST_MYSQL_PASSWORD') ?: '');
$db=['host'=>'127.0.0.1','port'=>3306,'database'=>$database,'username'=>'root','password'=>$password,'charset'=>'utf8mb4'];
$pdo=new PDO('mysql:host=127.0.0.1;port=3306;charset=utf8mb4','root',$password,[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$created=false;
$passed=0;
function expect(bool $condition,string $message): void {
    if (!$condition) throw new RuntimeException($message);
}
function request(array $query): array {
    global $directory;
    $process=proc_open([PHP_BINARY,$directory.'/runner.php',json_encode($query)],
        [0=>['pipe','r'],1=>['pipe','w'],2=>['pipe','w']],$pipes);
    if (!is_resource($process)) throw new RuntimeException('Cannot run fixture request');
    fclose($pipes[0]);
    $out=stream_get_contents($pipes[1]); fclose($pipes[1]);
    $err=stream_get_contents($pipes[2]); fclose($pipes[2]);
    $exit=proc_close($process);
    if ($exit!==0) throw new RuntimeException('Fixture request failed: '.$err);
    return json_decode($out,true,512,JSON_THROW_ON_ERROR);
}
try {
    $pdo->exec("CREATE DATABASE `{$database}` CHARACTER SET utf8mb4");
    $created=true;
    $pdo->exec("USE `{$database}`");
    $pdo->exec('CREATE TABLE iplate_users (id INT PRIMARY KEY,api_token VARCHAR(64),is_active TINYINT)');
    $pdo->exec('CREATE TABLE reservas (id INT PRIMARY KEY,cliente_id INT,codigo VARCHAR(40),status VARCHAR(30),data_checkin DATE,data_checkout DATE)');
    $pdo->exec('CREATE TABLE hospedes (id INT PRIMARY KEY,nome VARCHAR(80))');
    $pdo->exec('CREATE TABLE reserva_hospedes (reserva_id INT,cliente_id INT,hospede_id INT)');
    $pdo->exec("INSERT INTO iplate_users VALUES (1,'fixture-token',1)");
    $pdo->exec("INSERT INTO reservas VALUES
        (1,1,'2505371','confirmada',DATE_ADD(CURDATE(),INTERVAL 7 DAY),DATE_ADD(CURDATE(),INTERVAL 9 DAY)),
        (2,1,'2505372','checkin',CURDATE(),DATE_ADD(CURDATE(),INTERVAL 2 DAY)),
        (3,1,'2505373','cancelada',CURDATE(),DATE_ADD(CURDATE(),INTERVAL 2 DAY))");
    $pdo->exec("INSERT INTO hospedes VALUES (1,'Pessoa de teste A'),(2,'Pessoa de teste B')");
    $pdo->exec('INSERT INTO reserva_hospedes VALUES (1,1,1),(1,1,2),(2,1,1),(3,1,1)');
    mkdir($directory.'/api',0700,true);
    copy(dirname(__DIR__).'/api/reservation-search.php',$directory.'/api/reservation-search.php');
    file_put_contents($directory.'/config.php','<?php return '.var_export(['hotelaria_db'=>$db,'reservation'=>['provider'=>'hotelaria_sql','suggestions_source'=>'hotelaria_sql']],true).';');
    file_put_contents($directory.'/db.php', <<<'PHP'
<?php
$config=require __DIR__.'/config.php';$d=$config['hotelaria_db'];
$pdo=new PDO('mysql:host=127.0.0.1;port=3306;dbname='.$d['database'].';charset=utf8mb4','root',$d['password'],[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]);
const TABLE_USERS='iplate_users';
PHP);
    file_put_contents($directory.'/runner.php', <<<'PHP'
<?php
$_SERVER['REQUEST_METHOD']='GET';$_GET=json_decode($argv[1],true);
ob_start();
register_shutdown_function(function(){ $body=ob_get_clean(); echo json_encode(['http'=>http_response_code() ?: 200,'json'=>json_decode($body,true)]); });
require __DIR__.'/api/reservation-search.php';
PHP);
    $r=request(['api_token'=>'fixture-token','term'=>'2505371','exact'=>'1']);
    expect($r['http']===200 && count($r['json']['items'])===1,'Exact lookup must find future reservation');
    expect($r['json']['items'][0]['reservation_code']==='2505371','Wrong exact result');
    expect($r['json']['items'][0]['guest_count']===2 && $r['json']['items'][0]['checkout_date']!=='','Lost guest count or checkout'); $passed++;
    $r=request(['api_token'=>'fixture-token','term'=>'250']);
    expect(array_column($r['json']['items'],'reservation_code')===['2505372'],"Autocomplete must keep today's arrivals"); $passed++;
    foreach (['2505373','9999999',"2505371' OR 1=1 --"] as $term) {
        $r=request(['api_token'=>'fixture-token','term'=>$term,'exact'=>'1']);
        expect($r['http']===200 && $r['json']['items']===[],'Unexpected reservation match'); $passed++;
    }
    foreach (['','invalid-token'] as $token) {
        $r=request(['api_token'=>$token,'term'=>'2505371','exact'=>'1']);
        expect($r['http']===401,'Lookup must enforce authentication'); $passed++;
    }
    $pdo->exec('DROP TABLE reservas');
    $r=request(['api_token'=>'fixture-token','term'=>'2505371','exact'=>'1']);
    expect($r['http']===503 && $r['json']['success']===false,'DB errors must not look like empty success');
    expect(!str_contains($r['json']['message'],'SQLSTATE'),'Database details exposed'); $passed++;
    echo "PASS: {$passed} iPlate endpoint checks using a temporary local database.\n";
} finally {
    if ($created && preg_match('/^aqv_iplate_test_[a-f0-9]{12}$/',$database)) $pdo->exec("DROP DATABASE `{$database}`");
    foreach (['api/reservation-search.php','config.php','db.php','runner.php'] as $file) {
        if (is_file($directory.'/'.$file)) unlink($directory.'/'.$file);
    }
    if (is_dir($directory.'/api')) rmdir($directory.'/api');
    if (is_dir($directory)) rmdir($directory);
}
