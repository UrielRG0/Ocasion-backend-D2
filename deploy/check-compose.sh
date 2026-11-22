#!/usr/bin/env bash
# Este archivo se ejecuta antes de leer configuraciones privadas.
set -euo pipefail
if ! docker compose version >/dev/null 2>&1; then
  echo 'Falta el complemento Docker Compose (docker compose) para este usuario.' >&2
  echo 'Instala el complemento siguiendo docs/CI-CD-DOM2-Ubuntu.md y vuelve a intentar.' >&2
  echo 'No es necesario reinstalar Docker ni modificar MySQL.' >&2
  exit 1
fi
