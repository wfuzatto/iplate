# iPlate

Sistema para controle de entrada de veículos em hotel, composto por backend PHP/MySQL e aplicativo Android em Kotlin/Jetpack Compose.

## Estrutura

- `backend/`: painel web, autenticação, consulta de reservas e API de recebimento do app.
- `android/`: aplicativo Android para operação e registro das entradas.

## Backend (XAMPP / PHP + MySQL)

1. Crie o banco com `backend/database/iplate.sql`.
2. Se estiver atualizando uma instalação antiga, aplique `backend/database/iplate_auth_migration.sql`.
3. Copie `backend/config.local.example.php` para `backend/config.local.php` e ajuste as credenciais locais. Esse arquivo é ignorado pelo Git.
4. Alternativamente, configure as variáveis `IPLATE_DB_HOST`, `IPLATE_DB_PORT`, `IPLATE_DB_NAME`, `IPLATE_DB_USER` e `IPLATE_DB_PASSWORD`.
5. Abra `backend/` pelo servidor web.

O backend também aceita as variáveis `HOTELARIA_DB_HOST`, `HOTELARIA_DB_PORT`, `HOTELARIA_DB_NAME`, `HOTELARIA_DB_USER` e `HOTELARIA_DB_PASSWORD` para integração com a base de hotelaria.

## Usuário administrativo inicial

Na primeira inicialização, quando necessário, o backend cria o usuário `admin`. Defina `IPLATE_ADMIN_PASSWORD` no ambiente antes de subir o sistema. Se a variável não for definida, a senha temporária será `change-me-now` e deverá ser alterada imediatamente.

## App Android

1. Abra `android/` no Android Studio.
2. Sincronize o Gradle.
3. Em aparelho físico, ajuste no próprio aplicativo os endpoints do servidor e da integração Expresso.
4. Credenciais de integração não ficam armazenadas no repositório; devem ser informadas na configuração local do app.

## Dados e arquivos locais

Fotos em `backend/uploads/`, builds Android, `.gradle/`, `local.properties`, arquivos `.DS_Store` e configurações locais com segredos não são versionados.

## Segurança

Nunca faça commit de senhas de banco, tokens, credenciais da API Expresso, fotos reais de hóspedes/veículos ou arquivos `config.local.php`.

## Busca de reservas — integracao Expresso

O backend usa por padrao o **mesmo contrato do Acqua Vale Vendas**:
1. POST JSON `/api/obter_token` com `{"cpf":"...","password":"..."}`.
2. POST JSON `/api/reserva` com `{"token":"...","numero_reserva":"..."}`.
3. API iPlate `backend/api/reservation-search.php?api_token=...&term=2505371&exact=1`
   mantem o retorno `{"success":true,"items":[...]}`, compatível com o app.

Configure as variaveis **no servidor**: `IPLATE_EXPRESSO_CPF`,
`IPLATE_EXPRESSO_PASSWORD`. Alternativamente, use as chaves
`expresso.cpf` e `expresso.password` em `backend/config.local.php`.
Nao copie credenciais de producao para o repositorio.

O Expresso so oferece neste contrato busca pelo codigo **completo**.
A busca parcial / sugestoes retorna lista vazia em instalacoes novas; nao e erro.
Para preservar o autocomplete com o banco antigo, configure
`IPLATE_RESERVATION_SUGGESTIONS=hotelaria_sql` apenas se as tabelas
`reservas`, `reserva_hospedes` e `hospedes` ainda existirem.
Para retornar integralmente ao provedor antigo, use
`IPLATE_RESERVATION_PROVIDER=hotelaria_sql`. A nova busca exata
nao depende das tabelas antigas e nao limita a data de check-in a hoje.

O app Android usa o campo rotulado "CPF da API Expresso" na configuracao
existente e envia `cpf` para obtencao de token tanto na consulta
quanto no envio de anexos. Atualize/recompile o APK para usar esse ajuste.
Teste local sem rede: `php backend/tests/expresso-reservation-test.php`.
