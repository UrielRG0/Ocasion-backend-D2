package com.ocasion.backend.salones.Controllers;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ResponseStatusException error) {
        return respuesta(error.getStatusCode().value(), error.getReason() == null ? "Solicitud rechazada" : error.getReason());
    }
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> archivoFaltante(MissingServletRequestPartException error) {
        return respuesta(400, "Falta el archivo obligatorio: " + error.getRequestPartName());
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> parametroFaltante(MissingServletRequestParameterException error) {
        return respuesta(400, "Falta el parámetro obligatorio: " + error.getParameterName());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException error) {
        var campos = error.getBindingResult().getFieldErrors().stream()
            .map(campo -> campo.getField() + ": " + campo.getDefaultMessage()).distinct().toList();
        return ResponseEntity.badRequest().body(Map.of("status", 400, "mensaje", "Datos inválidos", "errores", campos));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflictoDatos(DataIntegrityViolationException error) {
        org.slf4j.LoggerFactory.getLogger(ApiErrorHandler.class).error("Conflicto al guardar datos", error);
        return respuesta(409, "No se pudieron guardar los datos por una restricción de la base de datos. Revise el registro del servidor.");
    }
    private ResponseEntity<Map<String, Object>> respuesta(int status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("status", status, "mensaje", mensaje));
    }
}
