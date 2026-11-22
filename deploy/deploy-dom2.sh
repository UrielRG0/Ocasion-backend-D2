#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
repo_dir="$(cd -- "$(dirname -- "$0")/.." && pwd -P)"
bash "$repo_dir/deploy/check-compose.sh"
artifact_dir="$(realpath -- "${1:?Indica la carpeta con el JAR probado}")"
state_dir="$(realpath -m -- "${DOM2_STATE_DIR:-$HOME/ocasion-dom2}")"
[[ "$state_dir" != / ]] || exit 1
env_file="$state_dir/.env"
[[ -f "$env_file" && "$(stat -c '%a' "$env_file")" == 600 ]] || { echo 'Prepara ~/ocasion-dom2/.env con permisos 600.'; exit 1; }
command -v flock >/dev/null; command -v curl >/dev/null; command -v python3 >/dev/null
docker info >/dev/null 2>&1 || { echo 'El runner necesita acceso a Docker sin sudo.'; exit 1; }
mkdir -p "$state_dir/releases"
exec 9>"$state_dir/.deploy.lock"
flock -w 120 9
export DOM2_ENV_FILE="$env_file"
compose_json="$(docker compose --env-file "$env_file" -f "$repo_dir/compose.yml" config --format json)" || { echo 'Docker Compose no pudo validar la configuracion.' >&2; exit 1; }
settings="$(printf '%s' "$compose_json" | python3 "$repo_dir/deploy/config_values.py")"
unset compose_json
mapfile -t config <<< "$settings"
bind_ip="${config[0]}"; port="${config[1]}"; network="${config[2]}"; database="${config[3]}"
ip -o -4 address show | awk '{print $4}' | cut -d/ -f1 | grep -Fxq "$bind_ip" || { echo 'La IP de ZeroTier no está asignada.'; exit 1; }
docker network inspect "$network" >/dev/null 2>&1 || { echo 'Falta la red Docker; ejecuta deploy/prepare-vm.sh.'; exit 1; }
[[ "$(docker inspect -f '{{.State.Running}}' "$database")" == true ]] || { echo 'MySQL existente no está encendido.'; exit 1; }
docker inspect -f '{{json .NetworkSettings.Networks}}' "$database" | python3 -c 'import json,sys; sys.exit(0 if sys.argv[1] in json.load(sys.stdin) else 1)' "$network" || { echo 'MySQL no está conectado a la red del backend.'; exit 1; }
shopt -s nullglob
jars=("$artifact_dir"/*.jar)
[[ ${#jars[@]} == 1 ]] || { echo 'Se requiere exactamente un JAR probado.'; exit 1; }
release_id="${GITHUB_SHA:-manual}-$(date +%s)-${GITHUB_RUN_ID:-0}-${GITHUB_RUN_ATTEMPT:-1}"
[[ "$release_id" =~ ^[a-zA-Z0-9._-]+$ ]] || exit 1
release="$state_dir/releases/$release_id"
mkdir -p "$release/context/.deploy-artifact"
touch "$release/.dom2-release"
install -m 644 "${jars[0]}" "$release/context/.deploy-artifact/app.jar"
cp "$repo_dir/Dockerfile" "$repo_dir/.dockerignore" "$release/context/"
cp "$repo_dir/compose.yml" "$release/compose.yml"
image="ocasion-dom2:$release_id"
python3 -c 'import json,sys; json.dump({"image":sys.argv[1]},open(sys.argv[2],"w"))' "$image" "$release/release.json"
docker build --pull --target ci-runtime --label com.ocasion.service=dom2 -t "$image" "$release/context"
# El JAR ya está en la imagen; no duplicarlo en cada release conservado.
rm -- "$release/context/.deploy-artifact/app.jar"
previous_image=''; previous_compose=''
if docker inspect ocasion-dom2 >/dev/null 2>&1; then
  [[ "$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' ocasion-dom2)" == ocasion-dom2 ]] || { echo 'Existe un contenedor ajeno llamado ocasion-dom2; no se modificó.'; exit 1; }
  [[ -L "$state_dir/current" ]] || { echo 'Falta el registro del despliegue previo. No se reemplazó el contenedor.'; exit 1; }
  previous_compose="$(readlink -f "$state_dir/current")/compose.yml"
  previous_image="$(docker inspect -f '{{.Config.Image}}' ocasion-dom2)"
fi
compose_up() {
  DOM2_IMAGE="$1" docker compose --project-name ocasion-dom2 --env-file "$env_file" -f "$2" up -d --no-deps --wait --wait-timeout 180 backend
}
rollback() {
  trap - ERR INT TERM
  echo 'La publicación falló; se intentará restaurar la imagen anterior.'
  if [[ -n "$previous_image" ]]; then
    if compose_up "$previous_image" "$previous_compose"; then echo 'Imagen anterior restaurada.'; else echo 'La restauración requiere revisión manual.'; fi
  else
    DOM2_IMAGE="$image" docker compose --project-name ocasion-dom2 --env-file "$env_file" -f "$release/compose.yml" rm -sf backend || true
  fi
  echo 'La base y su volumen no se eliminaron. El rollback de imagen no revierte cambios del esquema.'
  python3 "$repo_dir/deploy/prune_releases.py" "$state_dir" || true
  exit 1
}
trap rollback ERR INT TERM
compose_up "$image" "$release/compose.yml"
curl --noproxy '*' --fail --silent --show-error --max-time 10 "http://$bind_ip:$port/healthz" | python3 -c 'import json,sys; sys.exit(0 if json.load(sys.stdin).get("status")=="UP" else 1)'
trap - ERR INT TERM
if [[ -L "$state_dir/current" ]]; then
  ln -sfn "$(readlink -f "$state_dir/current")" "$state_dir/previous"
fi
ln -sfn "$release" "$state_dir/current"
python3 "$repo_dir/deploy/prune_releases.py" "$state_dir" || echo 'Limpieza pendiente; el servicio está publicado.'
echo "DOM2 publicado y conectado a MySQL: http://$bind_ip:$port"
