# Publicación y verificación de salones

La verificación se solicita después de publicar. Publicación y revisión documental son estados independientes.

## Recorrido
1. Vendedor: crear el salón y completar ubicación, servicios e inventario.
2. PATCH /api/v1/salones/{id}/publicar: PUBLICADO, sin exigir documentos.
3. POST /api/v1/salones/{id}/documentos (multipart): sólo propietario de un salón PUBLICADO. Guarda el expediente completo como SIN_ENVIAR; no entra en la bandeja todavía.
4. PATCH /api/v1/salones/{id}/solicitar-validacion: expediente completo pasa a PENDIENTE, salón permanece PUBLICADO.
5. Admin ROLE_Admin: GET /api/v1/admin/salones/pendientes y GET /{id}/expediente.
6. GET /api/v1/admin/salones/{id}/archivo/{tipo}[/{indice}]: autenticado con ROLE_Admin. Devuelve bytes PDF/JPG/PNG, no JSON.
7. PATCH /api/v1/admin/salones/{id}/validar: {"veredicto":"APROBADO"} o {"veredicto":"RECHAZADO","observaciones":"Motivo"}. Conserva PUBLICADO.
8. Tras rechazo se puede cargar nuevamente el expediente completo y reenviar. No se reemplazan documentos PENDIENTES o APROBADOS.

Campos multipart: ine, propiedad, comprobante, vobo, fotosEstablecimiento (1 a 10) y fotosPropietario (mínimo 3). Máximo 5 MiB por archivo. INE y fotos: JPG/PNG. Otros documentos: PDF/JPG/PNG. Repetir la clave para varias fotos.

El catálogo, detalle público y entidad del propietario incluyen verificado, derivado exclusivamente del expediente APROBADO. No requiere columna ni migración de esquema. El detalle administrativo conserva estado (publicación) y añade estadoRevisionAdmin.

## Errores esperados
- 403: rol incorrecto o vendedor no propietario.
- 409: cargar/solicitar antes de publicar, solicitud duplicada, modificación documental pendiente/aprobada, o veredicto fuera de revisión.
- 422: expediente incompleto al solicitar o aprobar; requisitos de publicación incompletos.
- 400: rechazo sin motivo, cantidad de fotos inválida.
- 413: tamaño excesivo; 415: formato no admitido.

## Registros de la versión anterior
No se modificaron registros existentes ni se ejecutó una migración automática. Los salones antiguos PENDIENTE_VALIDACION o RECHAZADO no se convierten automáticamente en anuncios públicos. Revisar cada caso con su propietario; si desea publicarlo, utilizar la operación publicar, que valida sus requisitos. Si el expediente antiguo ya está PENDIENTE, al publicar aparecerá en la bandeja; un expediente RECHAZADO puede corregirse y reenviarse. Crear salones nuevos de prueba es la forma más clara de verificar el recorrido completo.

## Ejecución y pruebas
Reiniciar la aplicación usando el launch.json habitual; no se cambió su configuración ni las propiedades de producción. El starter de validación ya estaba instalado.
Las pruebas de contexto usan H2 en memoria con configuración explícita; no acceden a MySQL. Se cubren persistencia de documentos, permisos, bandeja por estado documental, publicación sin verificación y rechazo/corrección/aprobación conservando visibilidad.

La vista debe usar verificado para mostrar el distintivo y estadoRevisionAdmin para mostrar el progreso de revisión. La conexión completa del formulario del vendedor sigue siendo trabajo del frontend.
