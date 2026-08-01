#Requires -Version 5.1
<#
.SYNOPSIS
  Aplica o patch de questões/simulados no backend Magistratura.

.DESCRIPTION
  Copia IaJsonExtractor, QuestaoService, SimuladoService,
  GlobalExceptionHandler e questao.txt para os caminhos correctos.

.PARAMETER BackendRoot
  Pasta do backend (a que contém pom.xml e src/).
  Se omitido, tenta detetar a partir da localização deste script
  ou pede ao utilizador.

.EXAMPLE
  .\aplicar-patch.ps1 -BackendRoot "C:\projetos\magistratura\backend"

.EXAMPLE
  # Coloca esta pasta backend-patch DENTRO do backend e corre:
  .\aplicar-patch.ps1
#>

param(
    [string]$BackendRoot = ""
)

$ErrorActionPreference = "Stop"

function Write-Info($msg)  { Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "[OK]    $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "[AVISO] $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "[ERRO]  $msg" -ForegroundColor Red }

# Pasta onde está este script (= conteúdo do ZIP backend-patch)
$PatchRoot = $PSScriptRoot
if (-not $PatchRoot) { $PatchRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }

Write-Info "Pasta do patch: $PatchRoot"

# --- Resolver BackendRoot ---
if (-not $BackendRoot) {
    # 1) Este script está dentro de .../backend/backend-patch ?
    $candidato = Split-Path -Parent $PatchRoot
    if (Test-Path (Join-Path $candidato "pom.xml")) {
        $BackendRoot = $candidato
        Write-Info "Backend detectado (pasta pai): $BackendRoot"
    }
}

if (-not $BackendRoot) {
    # 2) Pasta actual
    if (Test-Path ".\pom.xml") {
        $BackendRoot = (Resolve-Path ".").Path
        Write-Info "Backend detectado (pasta actual): $BackendRoot"
    }
}

if (-not $BackendRoot) {
    Write-Host ""
    Write-Host "Indica o caminho completo da pasta backend (onde está o pom.xml):"
    Write-Host "Exemplo: C:\Users\Tu\magistratura\backend"
    $BackendRoot = Read-Host "BackendRoot"
}

$BackendRoot = $BackendRoot.Trim().Trim('"')
if (-not (Test-Path (Join-Path $BackendRoot "pom.xml"))) {
    Write-Err "Nao encontrei pom.xml em: $BackendRoot"
    Write-Err "Indica a pasta correcta do backend."
    exit 1
}

Write-Ok "Backend: $BackendRoot"

# --- Mapa origem → destino (relativo ao backend) ---
$ficheiros = @(
    @{
        Src = "src\main\java\ao\magistratura\ia\IaJsonExtractor.java"
        Dst = "src\main\java\ao\magistratura\ia\IaJsonExtractor.java"
        Novo = $true
    },
    @{
        Src = "src\main\java\ao\magistratura\service\QuestaoService.java"
        Dst = "src\main\java\ao\magistratura\service\QuestaoService.java"
        Novo = $false
    },
    @{
        Src = "src\main\java\ao\magistratura\service\SimuladoService.java"
        Dst = "src\main\java\ao\magistratura\service\SimuladoService.java"
        Novo = $false
    },
    @{
        Src = "src\main\java\ao\magistratura\exception\GlobalExceptionHandler.java"
        Dst = "src\main\java\ao\magistratura\exception\GlobalExceptionHandler.java"
        Novo = $false
    },
    @{
        Src = "src\main\resources\prompts\questao.txt"
        Dst = "src\main\resources\prompts\questao.txt"
        Novo = $false
    }
)

$backupDir = Join-Path $BackendRoot ("_backup_patch_questoes_" + (Get-Date -Format "yyyyMMdd_HHmmss"))
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
Write-Info "Backup em: $backupDir"

$copiados = 0
$falhas = 0

foreach ($f in $ficheiros) {
    $origem = Join-Path $PatchRoot $f.Src
    $destino = Join-Path $BackendRoot $f.Dst

    if (-not (Test-Path $origem)) {
        Write-Err "Falta no patch: $($f.Src)"
        $falhas++
        continue
    }

    $destDir = Split-Path -Parent $destino
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        Write-Info "Criada pasta: $destDir"
    }

    # Backup se o destino já existe
    if (Test-Path $destino) {
        $relBackup = $f.Dst -replace '[\\/]', '_'
        Copy-Item -Path $destino -Destination (Join-Path $backupDir $relBackup) -Force
        Write-Info "Backup: $($f.Dst)"
    } elseif (-not $f.Novo) {
        Write-Warn "Destino nao existia (sera criado): $($f.Dst)"
    }

    Copy-Item -Path $origem -Destination $destino -Force
    Write-Ok "Aplicado: $($f.Dst)"
    $copiados++
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Copiados: $copiados  |  Falhas: $falhas" -ForegroundColor Cyan
Write-Host "  Backup:   $backupDir" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($falhas -gt 0) {
    Write-Err "Patch incompleto. Verifica a estrutura do ZIP."
    exit 1
}

Write-Ok "Patch aplicado com sucesso."
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Yellow
Write-Host "  1. Reinicia o backend (para o processo e volta a correr mvn spring-boot:run)"
Write-Host "  2. No browser: Ctrl+F5"
Write-Host "  3. Testa Gerar questoes com um ARTIGO concreto e quantidade 3"
Write-Host ""
