# DOM2 en Docker y GitHub Actions — guía de puesta en marcha

Destino confirmado: Ubuntu `ddbsadmin@172.30.219.232` y contenedor MySQL existente
`ocasion_db_dom2`. La base del proyecto es `ocasion_salon_db`; confirmar que ese
sea el nombre que contiene los datos actuales. No crear otra base ni recrear el
contenedor MySQL.

## 1. Qué queda automatizado

- Pull request hacia `master`: pruebas Java 21, H2, seguridad, validación del
  despliegue y arranque de la imagen con un MySQL 8 temporal en GitHub.
- Push/merge a `master`: esas mismas comprobaciones y luego despliegue en Ubuntu.
- También se puede ejecutar manualmente desde Actions, seleccionando `master`.
- El JAR probado se transfiere entre jobs con retención de un día. No se sube al
  repositorio. Ubuntu solo crea la imagen de ejecución con ese JAR; no recompila
  Java ni necesita Maven instalado.
- El proceso espera `/healthz`, que comprueba una conexión a MySQL. Ante un fallo
  intenta restaurar la imagen anterior. Esto no revierte cambios del esquema.
- La limpieza retiene los tres releases más recientes y protege las versiones
  actual/anterior. Solo elimina imágenes etiquetadas por este despliegue, sin
  forzar eliminación de imágenes en uso ni eliminar volúmenes de la VM.
- SAST y análisis de dependencias permanecen en el workflow de seguridad; las
  pruebas Java se centralizaron en el nuevo pipeline para no ejecutarlas dos veces.

El Compose de producción contiene únicamente el backend. El MySQL temporal del
pipeline pertenece a `deploy/compose.ci.yml`, en el runner de GitHub, y nunca se
conecta a la base de Ubuntu.

## 2. Revisar Ubuntu

En PowerShell:

```powershell
ssh ddbsadmin@172.30.219.232
```

Dentro de Ubuntu:

```bash
uname -m
df -h /
docker version
docker compose version
docker ps --format 'table {{.Names}}\t{{.Ports}}'
docker inspect --format '{{json .Mounts}}' ocasion_db_dom2
```

Si `docker compose version` falla o aparece `unknown flag: --env-file`, falta el
complemento Compose o no esta disponible para ese usuario. `docker-compose` con
guion es otro comando y no sustituye el complemento usado por estos scripts.

Actualiza el indice y consulta los paquetes disponibles:

```bash
sudo apt-get update
apt-cache policy docker-compose-plugin docker-compose-v2
```

Si `docker-compose-plugin` tiene una version candidata, instala:

```bash
sudo apt-get install docker-compose-plugin
```

Si el anterior no tiene candidato, pero `docker-compose-v2` si, usa el paquete de
Ubuntu:

```bash
sudo apt-get install docker-compose-v2
```

Instala solo una de las dos opciones y revisa la propuesta de APT antes de aceptar;
no debe retirar el motor Docker existente. Si ninguno tiene candidato, consulta
la instalacion manual del complemento en la [documentacion oficial de Docker](https://docs.docker.com/compose/install/linux/).
No purgues Docker ni borres contenedores o volumenes para instalar Compose.

Comprueba el resultado con la cuenta `ddbsadmin`, sin sudo:

```bash
docker compose version
docker compose up --help | grep -- --wait
```

Conserva el `.env` existente. Despues de instalar Compose, continua con la
preparacion de la red o con el workflow; no es necesario repetir la instalacion inicial.

Se necesita Linux x86_64, Docker y Docker Compose moderno con `up --wait`, además de
`curl`, `python3`, `ip`, `flock` y utilidades básicas de Ubuntu. El usuario del
runner debe poder ejecutar `docker info` sin sudo. Si falta ese permiso:

```bash
sudo usermod -aG docker ddbsadmin
```

Cierra la sesión SSH y vuelve a entrar para aplicar el grupo. Esto concede acceso
administrativo a Docker al usuario del runner; usa la cuenta autorizada del servidor.

El puerto elegido para DOM2 es **8080** de la VM, apuntando al **8081 interno** del
contenedor. Configura `DOM2_HOST_PORT=8080` en el `.env` privado y comprueba que
ese puerto este libre. El front conserva su 8082.

## 3. Copiar los archivos de preparación

La primera publicacion manual ya se realizo. Los siguientes despliegues quedan
a cargo del workflow. Estos pasos se conservan para preparar una VM nueva o
reparar la red de la existente; no hace falta repetirlos en cada publicacion.

Desde PowerShell en Windows, antes de subir el pipeline si todavía no está instalado
el runner de DOM2:

```powershell
ssh ddbsadmin@172.30.219.232 "mkdir -p ~/ocasion-dom2-setup"
scp "C:\salones\Dockerfile" "C:\salones\compose.yml" "C:\salones\.dockerignore" "C:\salones\.env.example" ddbsadmin@172.30.219.232:~/ocasion-dom2-setup/
scp -r "C:\salones\deploy" ddbsadmin@172.30.219.232:~/ocasion-dom2-setup/
```

Dentro de Ubuntu:

```bash
cd ~/ocasion-dom2-setup
python3 -m unittest discover -s deploy/tests -v
bash deploy/prepare-vm.sh
nano ~/ocasion-dom2/.env
```

`prepare-vm.sh` crea una plantilla privada solo si todavia no existe. Completa:

- `DB_USERNAME` y `DB_PASSWORD`: usuario y contraseña que ya permiten acceder a la
  base MySQL existente. No son necesariamente los del usuario SSH de Ubuntu.
- `JWT_SECRET`: exactamente el secreto actual compartido con DOM1. Cambiarlo por
  uno nuevo impediría validar los tokens de las cuentas que inician sesión allí.
- `DB_URL`: por defecto usa el nombre Docker `ocasion_db_dom2`, puerto interno 3306
  y la base `ocasion_salon_db`. No usar `localhost`: dentro del backend apuntaría
  al propio contenedor, no a MySQL.
- `DOM2_BIND_IP`, `DOM2_HOST_PORT`, `DOM2_DB_CONTAINER`: revisar que correspondan
  al servidor y al contenedor actuales. Mantener el resto salvo necesidad concreta.

El archivo `.env` debe permanecer en Ubuntu; no copiarlo a GitHub ni compartirlo.
Las contraseñas con `$` o `#` pueden escribirse entre comillas simples, como en la
plantilla. El archivo lo interpreta Compose; no ejecutar `source .env`.

Después de guardar:

```bash
chmod 600 ~/ocasion-dom2/.env
bash deploy/prepare-vm.sh
```

El script valida los datos, crea la red `ocasion-dom2-net` si no existe y conecta
MySQL a ella sin reiniciarlo ni mover su volumen. DOM2 se conectará a esa red al
desplegarse. Si MySQL rechaza al usuario desde la nueva red, revisar sus permisos
para esa base; no borrar/recrear la base para resolver un error de acceso.

La aplicación ahora toma las credenciales de variables de entorno. Si se ejecuta
fuera de Docker, también necesita `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`
en su configuración de ejecución. No quedan valores reales dentro del JAR nuevo.

`C:\salones\.vscode\launch.json` es configuracion local y se excluye del repositorio.
Para ejecutar desde VS Code, la configuracion Java debe definir las variables
anteriores. En Windows, `DB_URL` usa `172.30.219.232:3306`; el nombre Docker
`ocasion_db_dom2` se utiliza solo entre contenedores de la red de la VM.

## 4. Registrar un runner para el repositorio DOM2

El runner del front está asociado al repositorio del front; no sirve directamente
para `UrielRG0/Ocasion-backend-D2`. Pide a la persona propietaria que abra:

`Settings → Actions → Runners → New self-hosted runner → Linux → x64`

Debe facilitarte las instrucciones actuales de descarga y un token de registro
vigente. Descarga y extrae en una carpeta distinta de la del runner del front:

```bash
mkdir -p ~/actions-runner-dom2
cd ~/actions-runner-dom2
# Ejecutar aquí la descarga, validación y extracción que muestre GitHub.
./config.sh --url https://github.com/UrielRG0/Ocasion-backend-D2 --name ocasion-dom2 --labels ocasion-dom2
```

Introduce el token cuando lo pida. No uses sudo para `config.sh`. La etiqueta
`ocasion-dom2` debe escribirse exactamente así. Usa una versión actual del runner,
compatible con las acciones actuales, y conserva separada su carpeta `_work`.

Instálalo como servicio para que siga funcionando al cerrar SSH o reiniciar Ubuntu:

```bash
sudo ./svc.sh install ddbsadmin
sudo ./svc.sh start
sudo ./svc.sh status
```

GitHub debe mostrarlo **Idle**. Si no puedes ver Settings o registrar el runner,
la persona propietaria debe realizar esa operación inicial; poder hacer push no
otorga por sí solo el permiso de administrar runners.

## 5. Subir los cambios y ejecutar el pipeline

Revisa los cambios locales antes del commit: también hay cambios de negocio de
sesiones anteriores. El pipeline publica lo confirmado en `master`, no los archivos
que permanezcan sin commit en Windows.

Los archivos de esta automatización son:

- `.github/workflows/dom2-ci-cd.yml` y ajuste de `security-checks.yml`.
- `.github/actionlint.yaml` declara la etiqueta del runner al validador de workflows.
- `Dockerfile`, `.dockerignore`, `.gitattributes`, `.gitignore`, `compose.yml`, `.env.example`.
- `deploy/` y esta guía.
- `salones/pom.xml`, `application.properties`, `HealthController.java`, su prueba
  y el permiso GET `/healthz` en `SecurityConfig.java`.

Haz el commit después de revisarlos y envíalo a tu rama. Al integrarlo en `master`
se ejecutará `DOM2 - pruebas y despliegue`. Para una repetición manual:

`Actions → DOM2 - pruebas y despliegue → Run workflow → master`

No son necesarios secretos SSH, contraseñas MySQL ni el JWT en GitHub: el runner
lee el archivo privado de su VM. Solo se ejecuta el despliegue desde `master`; las
pruebas de los PR corren en GitHub, no en el runner que tiene acceso al servidor.

## 6. Verificar y cambiar la dirección del backend

En Ubuntu, cuando el pipeline termine:

```bash
docker ps --filter name=ocasion-dom2
docker inspect --format '{{.State.Health.Status}}' ocasion-dom2
curl --noproxy '*' -fsS http://172.30.219.232:8080/healthz
docker logs --tail 40 ocasion-dom2
```

La salud debe ser `healthy` y el endpoint devolver `{"status":"UP"}`. Probar además
login existente, catálogo, carga/lectura de fotos y documentos de prueba. Estos
datos permanecen en MySQL y no dependen de la vida del contenedor Java.

Solo después de comprobarlo, actualizar en el front:

```typescript
// src/app/core/config/api.config.ts
salones: 'http://172.30.219.232:8080',
```

Actualizar también Android, Postman y la configuración de DOM3 que consulten
DOM2. El cambio de URL no modifica el emisor de tokens ni las cuentas. Mantener
la instancia Windows hasta confirmar las pruebas; después detenerla para evitar
que dos instancias atiendan con distintas versiones sobre la misma base.

Las direcciones siguen siendo de la VPN ZeroTier. No son acceso público a Internet.

## Operación y recuperación

El último release está en `~/ocasion-dom2/current` y el anterior en
`~/ocasion-dom2/previous`. Sus `release.json` contienen etiquetas de imagen, sin
credenciales. Para una restauración manual, leer la etiqueta anterior y usarla:

```bash
cd ~/ocasion-dom2
export DOM2_ENV_FILE="$HOME/ocasion-dom2/.env"
export DOM2_IMAGE="ETIQUETA_ANTERIOR_DE_release.json"
docker compose -p ocasion-dom2 --env-file .env -f previous/compose.yml up -d --no-deps --wait backend
```

La restauración de imagen no deshace cambios JPA del esquema ni recupera datos
borrados. Disponer de un respaldo verificado antes de migraciones de esquema.
No usar `docker system prune --volumes` para liberar espacio en esta VM: aloja
bases de datos de varios servicios. Consultar el uso con `docker system df`.

El backend tiene reinicio automático y logs con rotación. Docker marca la salud,
pero `restart: unless-stopped` no reinicia por sí solo un contenedor únicamente
por estar `unhealthy`; el pipeline detecta ese estado al desplegar. El monitoreo
continuo y los respaldos de MySQL se configuran por separado.

Fuentes: [runners de GitHub](https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/add-runners),
[redes de Compose](https://docs.docker.com/compose/how-tos/networking/),
[configuración externa de Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).
