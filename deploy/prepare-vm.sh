#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir="$(cd -- "$(dirname -- "$0")/.." && pwd -P)"
bash "$repo_dir/deploy/check-compose.sh"
state_dir="$(realpath -m -- "${DOM2_STATE_DIR:-$HOME/ocasion-dom2}")"
[[ "$state_dir" != / ]] || exit 1
env_file="$state_dir/.env"
mkdir -p "$state_dir"
if [[ ! -f "$env_file" ]]; then
  install -m 600 "$repo_dir/.env.example" "$env_file"
  echo "Creado $env_file. Completa los valores con nano y vuelve a ejecutar este script."
  exit 0
fi
[[ "$(stat -c '%a' "$env_file")" == 600 ]] || { echo 'El archivo .env debe tener permisos 600.'; exit 1; }
docker info >/dev/null 2>&1 || { echo 'El usuario necesita Docker sin sudo. Revisa su grupo docker.'; exit 1; }
export DOM2_ENV_FILE="$env_file"
compose_json="$(docker compose --env-file "$env_file" -f "$repo_dir/compose.yml" config --format json)" || { echo 'Docker Compose no pudo validar la configuracion.' >&2; exit 1; }
settings="$(printf '%s' "$compose_json" | python3 "$repo_dir/deploy/config_values.py")"
unset compose_json
mapfile -t config <<< "$settings"
bind_ip="${config[0]}"; network="${config[2]}"; database="${config[3]}"
ip -o -4 address show | awk '{print $4}' | cut -d/ -f1 | grep -Fxq "$bind_ip" || { echo 'La IP de ZeroTier no está asignada a esta VM.'; exit 1; }
[[ "$(docker inspect -f '{{.State.Running}}' "$database")" == true ]] || { echo 'El contenedor MySQL existente debe estar encendido.'; exit 1; }
if ! docker network inspect "$network" >/dev/null 2>&1; then
  docker network create --label com.ocasion.service=dom2 "$network" >/dev/null
fi
if ! docker inspect -f '{{json .NetworkSettings.Networks}}' "$database" | python3 -c 'import json,sys; sys.exit(0 if sys.argv[1] in json.load(sys.stdin) else 1)' "$network"; then
  docker network connect "$network" "$database"
fi
echo "Red preparada. Se conserva MySQL y su volumen. DOM2 se publicará en http://${config[0]}:${config[1]}"
