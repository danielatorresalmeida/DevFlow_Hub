param(
    [string]$RepositoryRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$DatabaseHost = "localhost",
    [int]$DatabasePort = 5432,
    [string]$DatabaseName = "devflow_hub",
    [string]$DatabaseUser = "postgres"
)

$ErrorActionPreference = "Stop"

$migrationPath = Join-Path `
    $RepositoryRoot `
    "database\migrations\20260722_secure_collaborator_passwords.sql"

if (-not (Test-Path -LiteralPath $migrationPath)) {
    throw "Migration file not found: $migrationPath"
}

$psqlCommand = Get-Command psql -ErrorAction SilentlyContinue
$pgDumpCommand = Get-Command pg_dump -ErrorAction SilentlyContinue

if ($psqlCommand) {
    $psql = $psqlCommand.Source
}
else {
    $psql = Get-ChildItem `
        "C:\Program Files\PostgreSQL" `
        -Recurse `
        -Filter "psql.exe" `
        -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notlike "*pgAdmin 4*" } |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not $psql -or -not (Test-Path -LiteralPath $psql)) {
    throw "psql.exe was not found."
}

if ($pgDumpCommand) {
    $pgDump = $pgDumpCommand.Source
}
else {
    $pgDump = Join-Path (Split-Path -Parent $psql) "pg_dump.exe"
}

if (-not (Test-Path -LiteralPath $pgDump)) {
    throw "pg_dump.exe was not found beside psql.exe."
}

$securePassword = Read-Host "Password do PostgreSQL" -AsSecureString
$env:PGPASSWORD = [System.Net.NetworkCredential]::new(
    "",
    $securePassword
).Password

$backupDirectory = Join-Path $env:TEMP "DevFlow_Hub_Backups"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupPath = Join-Path `
    $backupDirectory `
    "collaborators-before-password-migration-$timestamp.dump"

New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null

try {
    Write-Host "[INFO] A criar backup da tabela collaborators..."

    & $pgDump `
        -h $DatabaseHost `
        -p $DatabasePort `
        -U $DatabaseUser `
        -d $DatabaseName `
        -F c `
        -t public.collaborators `
        -f $backupPath

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump terminou com código $LASTEXITCODE."
    }

    Write-Host "[PASS] Backup criado: $backupPath"
    Write-Host "[INFO] A executar migração transacional..."

    & $psql `
        -X `
        -v ON_ERROR_STOP=1 `
        -h $DatabaseHost `
        -p $DatabasePort `
        -U $DatabaseUser `
        -d $DatabaseName `
        -f $migrationPath

    if ($LASTEXITCODE -ne 0) {
        throw "psql terminou com código $LASTEXITCODE."
    }

    $validationSql = @'
SELECT
    COUNT(*) AS total,
    COUNT(*) FILTER (
        WHERE password LIKE '{bcrypt}$2a$%'
           OR password LIKE '{bcrypt}$2b$%'
           OR password LIKE '{bcrypt}$2y$%'
    ) AS bcrypt_passwords,
    COUNT(*) FILTER (
        WHERE password IS NULL
           OR BTRIM(password) = ''
           OR password !~ '^\{[A-Za-z0-9_-]+\}.+'
    ) AS invalid_passwords
FROM collaborators;

SELECT
    column_name,
    is_nullable,
    character_maximum_length
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'collaborators'
  AND column_name = 'password';

SELECT
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'public.collaborators'::regclass
  AND conname = 'collaborators_password_format_check';
'@

    & $psql `
        -X `
        -v ON_ERROR_STOP=1 `
        -h $DatabaseHost `
        -p $DatabasePort `
        -U $DatabaseUser `
        -d $DatabaseName `
        -P pager=off `
        -c $validationSql

    if ($LASTEXITCODE -ne 0) {
        throw "A validação terminou com código $LASTEXITCODE."
    }

    Write-Host "[PASS] Migração e validação concluídas"
    Write-Host "[INFO] Mantém o backup até concluir os testes da aplicação:"
    Write-Host $backupPath
}
finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
