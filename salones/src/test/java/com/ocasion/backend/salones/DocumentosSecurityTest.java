package com.ocasion.backend.salones;

import com.ocasion.backend.salones.Configs.*;
import com.ocasion.backend.salones.Controllers.DocumentosSalonController;
import com.ocasion.backend.salones.Services.DocumentacionSalonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentosSalonController.class)
@Import({SecurityConfig.class, JwtFilter.class})
@TestPropertySource(properties = "jwt.secret=dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA=")
class DocumentosSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean DocumentacionSalonService documentos;

    @Test void documentosPrivadosRechazanClientePropietarioYAnonimo() throws Exception {
        String url = "/api/v1/admin/salones/7/archivo/ine";
        mvc.perform(get(url)).andExpect(status().isForbidden());
        mvc.perform(get(url).with(user("9").roles("Cliente"))).andExpect(status().isForbidden());
        mvc.perform(get(url).with(user("9").roles("Vendedor"))).andExpect(status().isForbidden());
        verifyNoInteractions(documentos);
    }

    @Test void adminRecibeBinarioConTipoCorrectoYSinCache() throws Exception {
        byte[] pdf = "%PDF-1.7\ntest".getBytes();
        when(documentos.archivo(7,"propiedad",0)).thenReturn(pdf);
        mvc.perform(get("/api/v1/admin/salones/7/archivo/propiedad").with(user("1").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().bytes(pdf))
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void cargaMultipartUsaPropietarioDelJwtYRechazaCliente() throws Exception {
        when(documentos.guardar(eq(7),eq(9),any(),any(),any(),any(),anyList(),anyList()))
            .thenReturn(java.util.Map.of("id_salon",7));
        var peticion = multipart("/api/v1/salones/7/documentos");
        for (String parte : java.util.List.of("ine","propiedad","comprobante","vobo",
                "fotosEstablecimiento","fotosPropietario","fotosPropietario","fotosPropietario")) {
            peticion.file(new org.springframework.mock.web.MockMultipartFile(parte,"foto.jpg","image/jpeg",new byte[]{-1,-40,-1}));
        }
        mvc.perform(peticion.with(user("9").roles("Cliente"))).andExpect(status().isForbidden());
        String jwt = io.jsonwebtoken.Jwts.builder().claim("id_usuario",9)
            .claim("roles",java.util.List.of("ROLE_Vendedor"))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(
                "dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA="))).compact();
        // Usar una petición nueva para no conservar el usuario de prueba de la anterior.
        var autenticada = multipart("/api/v1/salones/7/documentos");
        for (String parte : java.util.List.of("ine","propiedad","comprobante","vobo",
                "fotosEstablecimiento","fotosPropietario","fotosPropietario","fotosPropietario")) {
            autenticada.file(new org.springframework.mock.web.MockMultipartFile(parte,"foto.jpg","image/jpeg",new byte[]{-1,-40,-1}));
        }
        mvc.perform(autenticada.header("Authorization","Bearer " + jwt))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id_salon").value(7));
        verify(documentos).guardar(eq(7),eq(9),any(),any(),any(),any(),argThat(f -> f.size() == 1),argThat(f -> f.size() == 3));
    }
}
