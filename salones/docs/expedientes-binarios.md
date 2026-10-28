# Documentos para revisión administrativa

Implementación del backend. Los archivos se envían como `multipart/form-data` y se guardan como bytes en MySQL, sin Base64 en JSON. La carga no publica el salón.

## 1. Subir o corregir el expediente

`POST /api/v1/salones/{id}/documentos`

Autenticación: `Authorization: Bearer <JWT>` con autoridad `ROLE_Vendedor`. El `id_usuario` del JWT debe coincidir con el propietario del salón. Solo admite salones en `BORRADOR` o `RECHAZADO`.

En Postman seleccionar Body → form-data y configurar cada campo como **File**:

| Campo | Cantidad | Formatos |
| --- | --- | --- |
| `ine` | 1 | JPG/JPEG o PNG |
| `propiedad` | 1 | PDF, JPG/JPEG o PNG |
| `comprobante` | 1 | PDF, JPG/JPEG o PNG |
| `vobo` | 1 | PDF, JPG/JPEG o PNG |
| `fotosEstablecimiento` | De 1 a 10 | JPG/JPEG o PNG |
| `fotosPropietario` | Al menos 3 | JPG/JPEG o PNG |

Repetir el mismo nombre de campo por cada foto. No enviar las fotos como un arreglo JSON. Dejar que Postman o el navegador genere el encabezado `Content-Type` con su boundary.

Límites: 5 MiB por archivo, 100 MiB por petición y 100 partes multipart. El límite total también limita la cantidad de fotos del propietario. Se revisan extensión y firma de contenido; esta revisión no certifica la validez legal de los documentos.

La petición incluye **todo el expediente**, también al corregir un rechazo. Reemplaza los documentos y fotos anteriores en una transacción; las fotos sobrantes se eliminan. Un archivo inválido no reemplaza el expediente guardado.

Respuesta `201 Created` de ejemplo:

```json
{
  "id_salon": 12,
  "id_expediente": 3,
  "estadoRevisionAdmin": "PENDIENTE",
  "estadoSalon": "BORRADOR",
  "fotosEstablecimiento": 2,
  "fotosPropietario": 3,
  "mensaje": "Documentos guardados. Ya puede solicitar la revisión."
}
```

Errores: `400` campos faltantes, archivos vacíos o cantidad incorrecta; `403` rol/propietario incorrecto; `404` salón inexistente; `409` estado incompatible; `413` exceso de tamaño; `415` formato no permitido. Los cuerpos de error pueden variar; comprobar primero el código HTTP.

## 2. Enviar a revisión

`PATCH /api/v1/salones/{id}/solicitar-validacion`

Mismo JWT del propietario, sin body. Verifica pertenencia, estado `BORRADOR`/`RECHAZADO`, ubicación, servicios, categoría con inventario y expediente completo. Faltantes producen `422`. En éxito responde `200`, asigna `PENDIENTE_VALIDACION` al salón y `PENDIENTE` al expediente, y borra observaciones anteriores. El salón aparece en la bandeja del administrador; no se implementan notificaciones push ni correo.

## 3. Revisar desde administración

Las siguientes rutas exigen JWT con autoridad `ROLE_ADMIN`. Un cliente o propietario recibe `403`:

- `GET /api/v1/admin/salones/pendientes?page=0&size=10`: bandeja existente, respuesta paginada (leer `content`).
- `GET /api/v1/admin/salones/{id}/expediente`: expediente existente, disponible cuando el salón está pendiente de validación.
- `GET /api/v1/admin/salones/{id}/archivo/{tipo}`: descarga de un documento o de la primera foto.
- `GET /api/v1/admin/salones/{id}/archivo/{tipo}/{indice}`: descarga de las fotos adicionales; índices desde 1. El índice 0 equivale a la primera foto.

Tipos: `ine`, `propiedad`, `comprobante`, `vobo`, `foto-establecimiento`, `foto-propietario`. Solo las fotografías tienen índices adicionales.

El subobjeto `documentos` conserva los campos anteriores y añade dos arreglos:

```json
{
  "urlInePasaporte": "/api/v1/admin/salones/12/archivo/ine",
  "urlDocumentoPropiedad": "/api/v1/admin/salones/12/archivo/propiedad",
  "urlComprobanteDom": "/api/v1/admin/salones/12/archivo/comprobante",
  "urlVistoBueno": "/api/v1/admin/salones/12/archivo/vobo",
  "urlFotosEstablecimiento": "/api/v1/admin/salones/12/archivo/foto-establecimiento",
  "urlFotosPropietario": "/api/v1/admin/salones/12/archivo/foto-propietario",
  "urlsFotosEstablecimiento": ["/api/v1/admin/salones/12/archivo/foto-establecimiento"],
  "urlsFotosPropietario": [
    "/api/v1/admin/salones/12/archivo/foto-propietario",
    "/api/v1/admin/salones/12/archivo/foto-propietario/1",
    "/api/v1/admin/salones/12/archivo/foto-propietario/2"
  ]
}
```

Resolver estas rutas contra la base del **backend**, no la dirección de la página web. Para visualizar un archivo desde una web con JWT, descargarlo mediante una petición con Authorization y respuesta `Blob`, crear una URL de objeto para el visor y liberarla al cerrar. Un `<img src>` directo no añade automáticamente el token. No poner el JWT en la URL.

Las descargas llevan el MIME detectado (`image/jpeg`, `image/png` o `application/pdf`), `Content-Disposition: attachment`, `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`. Los binarios no se incluyen en las respuestas JSON de las entidades. Para archivos antiguos con firma desconocida se usa `application/octet-stream`.

## 4. Emitir veredicto

Se conserva `PATCH /api/v1/admin/salones/{id}/validar`, con el contrato existente:

```json
{"veredicto":"APROBADO"}
```

o

```json
{"veredicto":"RECHAZADO","observaciones":"El comprobante no corresponde a la dirección registrada"}
```

Solo procesa salones en `PENDIENTE_VALIDACION`. Aprobación exige expediente completo, lo marca `APROBADO`, limpia observaciones y cambia el salón a `PUBLICADO`. Rechazo exige motivo no vacío de hasta 255 caracteres y cambia ambos estados a `RECHAZADO`. El propietario puede volver a cargar el expediente completo y solicitar revisión. La carga, el envío a revisión y el veredicto bloquean la misma fila del salón durante su transacción para evitar decisiones simultáneas sobre el expediente.

## Almacenamiento y arranque

Se mantienen los seis campos `LONGBLOB` existentes de `DocumentosValidacionSalon`: cuatro documentos y la primera foto de cada tipo. Las fotos adicionales se guardan como `LONGBLOB` en la nueva entidad/tabla `FotoExpediente`, vinculada al expediente, con tipo y posición. No se guardan archivos permanentes en una carpeta del servidor.

El proyecto tiene `spring.jpa.hibernate.ddl-auto=update`; al reiniciar crea la tabla nueva si el usuario de MySQL tiene permisos de esquema. No se realizó migración ni escritura sobre la base real durante la implementación. Si el despliegue desactiva `ddl-auto`, debe incorporar la tabla a su migración. MySQL debe admitir paquetes de al menos el tamaño de la fila binaria: para seis archivos de 5 MiB, configurar `max_allowed_packet` con margen suficiente (por ejemplo 64 MiB). Los expedientes antiguos con una sola foto del propietario requieren recarga para cumplir el mínimo de tres.

## Alcance conservado

Por solicitud expresa, `PATCH /api/v1/salones/{id}/publicar` conserva su funcionamiento anterior y **todavía puede publicar directamente**. Por ello no se garantiza que toda publicación pase por administración mientras esa ruta siga disponible. El flujo documentado aquí usa `documentos → solicitar-validacion → admin/validar`. El frontend no se modificó; debe integrar las nuevas peticiones para usar este flujo.
