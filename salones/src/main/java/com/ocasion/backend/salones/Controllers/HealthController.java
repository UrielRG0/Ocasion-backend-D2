package com.ocasion.backend.salones.Controllers;

import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Readiness mínimo: aplicación y conexión a MySQL, sin exponer configuración. */
@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String,String>> health() {
        boolean disponible;
        try { disponible = Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class)); }
        catch (DataAccessException ex) { disponible = false; }
        return ResponseEntity.status(disponible ? 200 : 503).cacheControl(CacheControl.noStore())
            .body(Map.of("status", disponible ? "UP" : "DOWN"));
    }
}
