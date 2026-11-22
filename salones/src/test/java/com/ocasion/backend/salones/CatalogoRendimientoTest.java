package com.ocasion.backend.salones;

import com.ocasion.backend.salones.DTO.FiltroBusquedaDTO;
import com.ocasion.backend.salones.Entities.*;
import com.ocasion.backend.salones.Services.ClienteSalonService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:rendimiento;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
 "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.sql.init.mode=never", "spring.jpa.properties.hibernate.generate_statistics=true",
 "jwt.secret=dGVzdC1zZWNyZXQtb25seS1mb3ItYXV0b21hdGVkLXRlc3RzLTEyMzQ1Njc4OTA="})
@Transactional
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class CatalogoRendimientoTest {
 @Autowired EntityManager em;
 @Autowired ClienteSalonService service;
 @Autowired com.ocasion.backend.salones.Services.SalonService propietario;
 @Autowired com.ocasion.backend.salones.Services.AdminValidacionService admin;
 @Autowired com.ocasion.backend.salones.Controllers.ImagenSalonController imagenes;
 @Autowired com.ocasion.backend.salones.Repositories.SalonRepository salones;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;

 @Test void excluyePropiosAntesDePaginarYRechazaDetalleYCalendarioParaReservar() {
   Integer propioId = null;
   for (int n=0; n<5; n++) {
     var salon = new SalonEntity(); salon.setNombreSalon("Catálogo " + n);
     salon.setEstado("PUBLICADO"); salon.setUsuariosIdUser(n<2?9:8);
     salon.setPrecio_hora(BigDecimal.TEN); em.persist(salon);
     if(n==0) propioId=salon.getId_salon();
   }
   em.flush(); em.clear();
   var contextoAnterior = org.springframework.security.core.context.SecurityContextHolder.getContext();
   var contexto = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
   contexto.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
     "9", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_Cliente"))));
   org.springframework.security.core.context.SecurityContextHolder.setContext(contexto);
   try {
     var primera=service.buscarSalones(new FiltroBusquedaDTO(),PageRequest.of(0,2));
     var segunda=service.buscarSalones(new FiltroBusquedaDTO(),PageRequest.of(1,2));
     assertEquals(3,primera.getTotalElements()); assertEquals(2,primera.getNumberOfElements());
     assertEquals(1,segunda.getNumberOfElements());
     Integer id=propioId;
     assertTrue(primera.stream().noneMatch(s->s.getId_salon().equals(id)));
     assertEquals(403,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.obtenerDetalle(id)).getStatusCode().value());
     assertEquals(403,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.obtenerDisponibilidadMensual(id,11,2026)).getStatusCode().value());
     org.springframework.security.core.context.SecurityContextHolder.clearContext();
     assertEquals(5,service.buscarSalones(new FiltroBusquedaDTO(),PageRequest.of(0,10)).getTotalElements());
   } finally { org.springframework.security.core.context.SecurityContextHolder.setContext(contextoAnterior); }
 }

 @Test void seguimientoDePropietarioUsaUnaConsultaSinBinariosYNiegaOtroPropietario() {
   crearDatos();
   var id=em.createQuery("select s.id_salon from SalonEntity s order by s.id_salon",Integer.class).setMaxResults(1).getSingleResult();
   var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
   var resultado=documentos.estado(id,99);
   assertEquals("APROBADO",resultado.documentosValidacionSalonEntity().estadoRevisionAdmin());
   assertEquals(1,stats.getPrepareStatementCount()); assertEquals(0,stats.getEntityLoadCount());
   assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->documentos.estado(id,8)).getStatusCode().value());
 }
 @Autowired com.ocasion.backend.salones.Services.DocumentacionSalonService documentos;

 @Test void paginaNoCargaEntidadesNiDocumentosYAcotaConsultas() {
   crearDatos();
   var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
   var page=service.buscarSalones(new FiltroBusquedaDTO(),PageRequest.of(0,10));
   long queries=stats.getPrepareStatementCount(), entities=stats.getEntityLoadCount();
   System.out.println("CATALOGO_METRICAS consultas="+queries+" entidades="+entities);
   assertEquals(12,page.getTotalElements()); assertEquals(10,page.getNumberOfElements());
   assertTrue(page.getContent().stream().allMatch(x->x.isVerificado() && "Prueba".equals(x.getCiudad()) && x.getImagen_portada().startsWith("/api/v1/salones/")));
   assertEquals(0,entities,"El catálogo no debe materializar entidades con documentos binarios");
   assertTrue(queries<=4,"La cantidad de consultas no debe crecer por cada tarjeta");
   assertEquals(java.util.List.of("Meseros"),page.getContent().getFirst().getServiciosDestacados());
   var servicioId=em.createQuery("select s.idServicio from ServiciosEntity s where s.nombreServicio='Meseros'",Integer.class).getSingleResult();
   var filtro=new FiltroBusquedaDTO(); filtro.setCiudad("Prueba"); filtro.setServiciosIds(java.util.List.of(servicioId));
   var filtrada=service.buscarSalones(filtro,PageRequest.of(0,3));
   assertEquals(6,filtrada.getTotalElements()); assertEquals(3,filtrada.getNumberOfElements());
   assertTrue(filtrada.stream().allMatch(x->x.getServiciosDestacados().contains("Meseros")));
   var segunda=service.buscarSalones(new FiltroBusquedaDTO(),PageRequest.of(1,10));
   assertEquals(2,segunda.getNumberOfElements());
   assertTrue(segunda.stream().noneMatch(x->page.stream().anyMatch(y->x.getId_salon().equals(y.getId_salon()))));
 }

 private void crearDatos() {
   var servicio=new ServiciosEntity(); servicio.setNombreServicio("Meseros"); em.persist(servicio);
   var categoria=new CategoriaRecursoEntity(); categoria.setNombreCategoria("Mobiliario"); em.persist(categoria);
   for(int n=0;n<13;n++) {
     var s=new SalonEntity(); s.setNombreSalon("Rendimiento "+n); s.setEstado(n==12?"BORRADOR":"PUBLICADO");
     s.setUsuariosIdUser(99); s.setPrecio_hora(BigDecimal.TEN); s.setCapacidadPersonas(100); em.persist(s);
     var u=new UbicacionEntity(); u.setSalon(s); u.setCiudad("Prueba"); u.setDireccion("Calle"); u.setCP("20000");
     u.setLatitud(BigDecimal.ONE); u.setLongitud(BigDecimal.ONE); em.persist(u);
     var recurso=new InventarioSalonEntity(); recurso.setSalon(s); recurso.setCategoria(categoria);
     recurso.setNombreRecurso("Sillas"); recurso.setCantidadTotal(25); em.persist(recurso);
     var horario=new DisponibilidadEntity(); horario.setSalon(s); horario.setFecha(java.time.LocalDate.of(2026,11,10));
     horario.setHora_inicio(horario.getFecha().atTime(10,0)); horario.setHora_fin(horario.getFecha().atTime(20,0)); em.persist(horario);
     var d=new DocumentosValidacionSalonEntity(); d.setSalon(s); d.setEstadoRevisionAdmin("APROBADO");
     d.setInePropietarioPasaporte(new byte[1024*1024]); em.persist(d);
     var photo=new ImagenSalonEntity(); photo.setSalon(s); photo.setPosicion(0); photo.setContentType("image/png");
     photo.setTamano(1024); photo.setContenido(new byte[1024]); em.persist(photo);
     if(n%2==0) {
       var relacion=new SalonServiciosEntity(); relacion.setSalon(s); relacion.setServicio(servicio); em.persist(relacion);
     }
   }
   em.flush(); em.clear();
 }

 @Test void propietarioConservaContratoSinLeerBinariosNiOtrosPropietarios() throws Exception {
   crearDatos();
   var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
   // Reproduce la lectura anterior: entidades y relaciones usadas al construir el JSON.
   long bytesDocumentos=0;
   for(var anterior:salones.findByUsuariosIdUser(99)) {
     anterior.getImagenes(); anterior.getServiciosAsignados().size(); anterior.getInventario().size();
     anterior.getDisponibilidades().size();
     bytesDocumentos+=anterior.getDocumentosValidacionSalonEntity().getInePropietarioPasaporte().length;
   }
   System.out.println("PROPIETARIO_ANTES consultas="+stats.getPrepareStatementCount()+" entidades="+stats.getEntityLoadCount()+" bytesDocumentos="+bytesDocumentos);
   assertEquals(13L*1024*1024,bytesDocumentos);
   em.clear(); stats.clear();
   var lista=propietario.obtenerSalonesPorPropietario(99);
   long queries=stats.getPrepareStatementCount(), entities=stats.getEntityLoadCount();
   System.out.println("PROPIETARIO_METRICAS consultas="+queries+" entidades="+entities);
   assertEquals(13,lista.size()); assertEquals(0,entities); assertEquals(5,queries);
   var primero=lista.getFirst();
   assertTrue(primero.isVerificado()); assertEquals("APROBADO",primero.getDocumentosValidacionSalonEntity().estadoRevisionAdmin());
   assertEquals("Prueba",primero.getUbicacion().ciudad()); assertEquals("20000",primero.getUbicacion().cp());
   assertEquals("Meseros",primero.getServiciosAsignados().getFirst().servicio().nombreServicio());
   assertEquals("Sillas",primero.getInventario().getFirst().nombreRecurso());
   assertEquals(1,primero.getDisponibilidades().size()); assertEquals(1,primero.getImagenes().size());
   assertEquals(0,propietario.obtenerSalonesPorPropietario(100).size());
   var token=io.jsonwebtoken.Jwts.builder().setSubject("test").claim("id_usuario",99)
       .claim("roles",java.util.List.of("ROLE_Vendedor")).setExpiration(new java.util.Date(System.currentTimeMillis()+60000))
       .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(PentestDom2Test.KEY))).compact();
   mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/salones/propietario")
       .header("Authorization","Bearer "+token))
       .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
       .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].ubicacion.cp").value("20000"))
       .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].serviciosAsignados[0].servicio.nombreServicio").value("Meseros"))
       .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].documentosValidacionSalonEntity.estadoRevisionAdmin").value("APROBADO"))
       .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].documentosValidacionSalonEntity.inePropietarioPasaporte").doesNotExist());
 }

 @Test void bandejaPendientesNoCargaEntidadesNiBinarios() {
   crearDatos();
   em.createQuery("update DocumentosValidacionSalonEntity d set d.estadoRevisionAdmin='PENDIENTE'").executeUpdate();
   em.clear();
   var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
   salones.findByEstadoAndDocumentosValidacionSalonEntity_EstadoRevisionAdmin("PUBLICADO","PENDIENTE",PageRequest.of(0,10));
   long documentos=stats.getEntityStatistics(DocumentosValidacionSalonEntity.class.getName()).getLoadCount();
   System.out.println("ADMIN_ANTES consultas="+stats.getPrepareStatementCount()+" expedientes="+documentos);
   assertTrue(documentos>0,"Reproducir el costo de los expedientes al cargar entidades en la bandeja anterior");
   em.clear(); stats.clear();
   var pagina=admin.obtenerSalonesPendientes(PageRequest.of(0,10));
   System.out.println("ADMIN_METRICAS consultas="+stats.getPrepareStatementCount()+" entidades="+stats.getEntityLoadCount());
   assertEquals(12,pagina.getTotalElements()); assertEquals(10,pagina.getNumberOfElements());
   assertEquals(0,stats.getEntityLoadCount()); assertTrue(stats.getPrepareStatementCount()<=2);
   assertEquals("Propietario ID: 99",pagina.getContent().getFirst().getPropietarioNombre());
 }

 @Test void fotoLeeSoloElBinarioSolicitadoYProtegeBorrador() {
   crearDatos();
   var foto=em.createQuery("select f.salon.id_salon, f.id from ImagenSalonEntity f where f.salon.estado='BORRADOR'",Object[].class).getSingleResult();
   var stats=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); stats.clear();
   var denegado=assertThrows(org.springframework.web.server.ResponseStatusException.class,
       ()->imagenes.foto((Integer)foto[0],(Long)foto[1],null));
   assertEquals(404,denegado.getStatusCode().value()); assertEquals(1,stats.getPrepareStatementCount());
   var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("99",null,java.util.List.of());
   stats.clear(); var response=imagenes.foto((Integer)foto[0],(Long)foto[1],auth);
   assertEquals(1024,response.getBody().length); assertEquals(0,stats.getEntityLoadCount());
   assertEquals(2,stats.getPrepareStatementCount());
   System.out.println("FOTO_METRICAS consultas="+stats.getPrepareStatementCount()+" entidades="+stats.getEntityLoadCount());
 }
}
