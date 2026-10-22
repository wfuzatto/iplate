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
3. Atualize o backend PHP **antes** de instalar o APK novo e configure `IPLATE_EXPRESSO_CPF` e `IPLATE_EXPRESSO_PASSWORD` apenas no servidor.
4. Em aparelho físico, configure a URL do backend iPlate, entre com usuário/senha do operador e consulte uma reserva completa (exemplo fictício: `2505371`).
5. O Android consulta reservas **por POST autenticado para o backend iPlate**, sem receber CPF/senha nem token Expresso. A API não oferece busca parcial por nome; o aplicativo aceita leitura de QR Code e número completo.
6. **Compatibilidade de anexos:** a rotina legada `/api/anexo` ainda envia os anexos diretamente pelo Android e, por isso, exige CPF/senha Expresso nas configurações locais. Esses dados não devem ser incluídos no Git e **ainda ficam armazenados nas preferências locais do aplicativo**. Esta migração não altera o fluxo de anexos nem o substitui pelo webhook Vale Visitor.

## Dados e arquivos locais

Fotos em `backend/uploads/`, builds Android, `.gradle/`, `local.properties`, arquivos `.DS_Store` e configurações locais com segredos não são versionados.

## Segurança

Nunca faça commit de senhas de banco, tokens, credenciais da API Expresso, fotos reais de hóspedes/veículos ou arquivos `config.local.php`.

## Busca de reservas — integracao Expresso

O backend usa por padrao o **mesmo contrato do Acqua Vale Vendas**:
1. POST JSON `/api/obter_token` com `{"cpf":"...","password":"..."}`.
2. POST JSON `/api/reserva` com `{"token":"...","numero_reserva":"..."}`.
3. O APK atual envia POST JSON para `backend/api/reservation-search.php` com
   `{"api_token":"TOKEN_DO_OPERADOR_IPLATE","term":"2505371","exact":"1"}`.
   O token do operador fica **no corpo HTTPS**, não na URL. O backend mantém
   suporte GET para versões antigas do APK. O retorno continua
   `{"success":true,"items":[...]}` com `reservation_id`, `reservation_code`,
   `guest_name`, `guest_cpf`, `checkin_date`, `checkout_date`,
   `adults`, `children`, `guest_count`, `uh` e `status`.

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

No backend, o CPF é utilizado para obter token via `/api/obter_token`, e
a reserva é consultada por `/api/reserva`. O aplicativo não chama esses
endpoints diretamente ao consultar reservas. A consulta exige login no iPlate.
O campo "CPF da API Expresso (anexos)" do Android normaliza para 11 dígitos,
sendo usado apenas pelo fluxo legado de upload de fotos para `/api/anexo`.

**Atenção:** `/visitor/acquavale_receive.php` integra as vendas pagas ao
Vale Visitor; não é um endpoint de busca do iPlate.

Verificações sem credenciais de produção:
`php backend/tests/expresso-reservation-test.php` e
`./gradlew :app:assembleDebug` a partir de `android/`.
O workflow Android publica o APK de depuração como artefato somente após
compilação bem-sucedida. A consulta real e o upload de fotos requerem
homologação no servidor e aparelho Android.
