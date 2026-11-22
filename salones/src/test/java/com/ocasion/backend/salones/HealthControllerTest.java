package com.ocasion.backend.salones;

import com.ocasion.backend.salones.Configs.*;
import com.ocasion.backend.salones.Controllers.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtFilter.class})
@TestPropertySource(properties="jwt.secret=dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA=")
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;
    @Test void healthCompruebaLaBaseSinToken() throws Exception {
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        mvc.perform(get("/healthz")).andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"))
            .andExpect(header().string("Cache-Control","no-store"));
        verify(jdbc).queryForObject("SELECT 1",Integer.class);
    }
    @Test void unaFallaDeLaBaseNoSeDeclaraSaludableNiExponeCredenciales() throws Exception {
        when(jdbc.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessResourceFailureException("dato privado del servidor"));
        mvc.perform(get("/healthz")).andExpect(status().isServiceUnavailable())
            .andExpect(content().string("{\"status\":\"DOWN\"}"));
    }
}
