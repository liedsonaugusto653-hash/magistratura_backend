#!/usr/bin/env bash
set -euo pipefail
PATCH_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_ROOT="${1:-}"

if [[ -z "$BACKEND_ROOT" ]]; then
  if [[ -f "$PATCH_ROOT/../pom.xml" ]]; then
    BACKEND_ROOT="$(cd "$PATCH_ROOT/.." && pwd)"
  elif [[ -f "./pom.xml" ]]; then
    BACKEND_ROOT="$(pwd)"
  else
    echo "Uso: $0 /caminho/para/backend"
    exit 1
  fi
fi

if [[ ! -f "$BACKEND_ROOT/pom.xml" ]]; then
  echo "ERRO: pom.xml nao encontrado em $BACKEND_ROOT"
  exit 1
fi

BACKUP="$BACKEND_ROOT/_backup_patch_questoes_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP"
echo "[INFO] Backend: $BACKEND_ROOT"
echo "[INFO] Backup:  $BACKUP"

copy_one() {
  local rel="$1"
  local src="$PATCH_ROOT/$rel"
  local dst="$BACKEND_ROOT/$rel"
  if [[ ! -f "$src" ]]; then
    echo "[ERRO] Falta no patch: $rel"
    return 1
  fi
  mkdir -p "$(dirname "$dst")"
  if [[ -f "$dst" ]]; then
    cp "$dst" "$BACKUP/$(echo "$rel" | tr '/' '_')"
  fi
  cp "$src" "$dst"
  echo "[OK] $rel"
}

copy_one "src/main/java/ao/magistratura/ia/IaJsonExtractor.java"
copy_one "src/main/java/ao/magistratura/service/QuestaoService.java"
copy_one "src/main/java/ao/magistratura/service/SimuladoService.java"
copy_one "src/main/java/ao/magistratura/exception/GlobalExceptionHandler.java"
copy_one "src/main/resources/prompts/questao.txt"

echo ""
echo "Patch aplicado. Reinicia o backend."
