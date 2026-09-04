package com.ocasion.backend.salones.Configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity 
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF porque nuestra API es REST y usa tokens, no cookies
            .csrf(csrf -> csrf.disable())
            
            // Le decimos a Spring que no guarde estado (sesiones en memoria)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configuración de rutas
            .authorizeHttpRequests(auth -> auth
                // Ejemplo: Si quieres que cualquiera pueda ver los salones sin estar logueado
                .requestMatchers("/api/salones/publicos/**").permitAll()
                // Cualquier otra petición (como crear, editar o borrar) requerirá un token válido
                .anyRequest().authenticated()
            )
            
            // Colocamos nuestro filtro JWT ANTES del filtro estándar de Spring Security
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}