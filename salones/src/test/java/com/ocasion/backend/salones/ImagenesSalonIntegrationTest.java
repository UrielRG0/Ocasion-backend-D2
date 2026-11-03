package com.ocasion.backend.salones;

import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Repositories.SalonRepository;
import com.ocasion.backend.salones.Services.ImagenesSalon;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:fotos;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
 "spring.sql.init.mode=never", "jwt.secret=dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA="})
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class ImagenesSalonIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired SalonRepository salones;
 @Autowired EntityManager em;
 byte[] png;
 SalonEntity salon;
 @BeforeEach void preparar() throws Exception {
   var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out); png=out.toByteArray();
   salon=new SalonEntity(); salon.setNombreSalon("Fotos"); salon.setUsuariosIdUser(9);
   salon.setEstado("BORRADOR"); salon.setPrecio_hora(java.math.BigDecimal.TEN);
   salones.saveAndFlush(salon);
 }
 MockMultipartFile file() { return new MockMultipartFile("imagenes","foto.png","image/png",png); }
 String subir() throws Exception {
   mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,"/api/v1/salones/"+salon.getId_salon()+"/imagenes")
     .file(file()).with(user("9").roles("Vendedor"))).andExpect(status().isOk());
   em.flush(); em.clear(); salon=salones.findById(salon.getId_salon()).orElseThrow();
   return salon.getImagenes().getFirst();
 }
 @Test void persisteBytesYProtegeBorradoresSinExponerExpedientes() throws Exception {
   String url=subir();
   assertArrayEquals(png,salon.getFotosCatalogo().getFirst().getContenido());
   assertNull(salon.getImagen());
   em.clear(); salon=salones.findById(salon.getId_salon()).orElseThrow();
   salon.getImagenes();
   assertFalse(org.hibernate.Hibernate.isInitialized(salon.getFotosCatalogo().getFirst().getArchivo()),
     "Listar URLs no debe cargar binarios");
   mvc.perform(get(url)).andExpect(status().isNotFound());
   mvc.perform(get(url).with(user("10").roles("Vendedor"))).andExpect(status().isNotFound());
   mvc.perform(get(url).with(user("9").roles("Vendedor")))
     .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
   salon.setEstado("PUBLICADO"); em.flush();
   mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(png));
   mvc.perform(get("/api/v1/admin/salones/"+salon.getId_salon()+"/archivo/ine")).andExpect(status().isForbidden());
   mvc.perform(get("/api/v1/salones/propietario").with(user("9").roles("Vendedor")))
     .andExpect(status().isOk()).andExpect(jsonPath("$[0].imagenes[0]").value(url))
     .andExpect(jsonPath("$[0].fotosCatalogo").doesNotExist()).andExpect(jsonPath("$[0].imagen").doesNotExist());
 }
 @Test void rechazaOtrosPropietariosYArchivosFalsosSinBorrarFotos() throws Exception {
   String original=subir();
   var url="/api/v1/salones/"+salon.getId_salon()+"/imagenes";
   mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,url).file(file()).with(user("10").roles("Vendedor")))
     .andExpect(status().isForbidden());
   mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,url)
     .file(new MockMultipartFile("imagenes","foto.png","image/png","no es una foto".getBytes()))
     .with(user("9").roles("Vendedor"))).andExpect(status().isUnsupportedMediaType());
   assertEquals(original,salon.getImagenes().getFirst());
 }
 @Test void reemplazaGaleriaYEliminaBinariosHuerfanos() throws Exception {
   String anterior=subir();
   String nueva=subir();
   assertNotEquals(anterior,nueva);
   assertEquals(1L,em.createQuery("select count(f) from ImagenSalonEntity f",Long.class).getSingleResult());
   mvc.perform(get(anterior).with(user("9").roles("Vendedor"))).andExpect(status().isNotFound());
   salones.delete(salon); em.flush();
   assertEquals(0L,em.createQuery("select count(f) from ImagenSalonEntity f",Long.class).getSingleResult());
   assertEquals(0L,em.createQuery("select count(f) from ContenidoImagenSalonEntity f",Long.class).getSingleResult());
 }
 @Test void creaSalonYFotosMultipartEnUnaOperacion() throws Exception {
   String dto="""
     {"nombreSalon":"Nuevo","capacidadPersonas":40,"precio_hora":100,"descripcion":"Espacio de prueba",
      "ubicacion":{"direccion":"Calle 1","ciudad":"AGS","cp":"20000","latitud":21,"longitud":-102},
      "disponibilidadInicial":{"fecha":"2027-01-01","hora_inicio":"2027-01-01T10:00:00","hora_fin":"2027-01-01T20:00:00"}}
     """;
   mvc.perform(multipart("/api/v1/salones/fase1").file(file())
     .file(new MockMultipartFile("salon","","application/json",dto.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
     .with(user("9").roles("Vendedor"))).andExpect(status().isCreated());
   em.flush(); em.clear();
   assertEquals(1L,em.createQuery("select count(f) from ImagenSalonEntity f",Long.class).getSingleResult());
 }
 @Test void validaLimitesYCompatibilidadBase64SinSepararComas() {
   var file=file();
   assertThrows(org.springframework.web.server.ResponseStatusException.class,()->ImagenesSalon.leer(Collections.nCopies(6,file)));
   assertThrows(org.springframework.web.server.ResponseStatusException.class,()->ImagenesSalon.validar(new byte[ImagenesSalon.MAX_BYTES+1]));
   String data="data:image/png;base64,"+Base64.getEncoder().encodeToString(png);
   assertEquals(List.of(data,"https://example.test/foto.jpg"),ImagenesSalon.antiguas(data+",https://example.test/foto.jpg"));
   ImagenesSalon.desdeJson(salon,List.of(data));
   assertArrayEquals(png,salon.getFotosCatalogo().getFirst().getContenido());
   assertThrows(org.springframework.web.server.ResponseStatusException.class,
     ()->ImagenesSalon.desdeJson(salon,List.of("C:\\fotos\\salon.jpg")));
 }
}
