package com.ocasion.backend.salones.Configs;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            // =====================================================
            // MODIFICACIÓN CORS
            //
            // Ya tenías corsConfigurationSource(), pero faltaba
            // indicarle a Spring Security que debía utilizarlo.
            // =====================================================
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            )


            // =====================================================
            // CSRF
            //
            // Se desactiva porque trabajamos con una API REST
            // autenticada mediante JWT.
            // =====================================================
            .csrf(csrf ->
                csrf.disable()
            )


            // =====================================================
            // SESIONES
            //
            // JWT es stateless, por lo que Spring no debe crear
            // sesiones HTTP.
            // =====================================================
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )


            // =====================================================
            // AUTORIZACIÓN DE RUTAS
            // =====================================================
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                )
                .permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/salones/explorar",
                    "/api/v1/salones/*/detalle",
                    "/api/v1/salones/*/calendario",
                    "/api/v1/servicios"
                )
                .permitAll()

                .requestMatchers(
                    "/api/salones/publicos/**"
                )
                .permitAll()

                // ==========================================
                // MODIFICACIÓN ADMIN
                //
                // Actualmente el JWT genera ROLE_Admin.
                // Dejamos ROLE_ADMIN también preparado
                // para cuando normalicemos los roles.
                // ==========================================
                .requestMatchers(
                    "/api/v1/admin/**"
                )
                .hasAnyAuthority(
                    "ROLE_ADMIN",
                    "ROLE_Admin"
                )

                .anyRequest()
                .authenticated()
            )


            // =====================================================
            // FILTRO JWT
            // =====================================================
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );


        return http.build();
    }


    // =========================================================
    // CONFIGURACIÓN GLOBAL DE CORS
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();


        // =====================================================
        // Frontends permitidos
        //
        // Tu error actual viene desde:
        // http://localhost:4200
        //
        // También dejamos la IP que ya utilizabas.
        // =====================================================

        configuration.setAllowedOrigins(
            Arrays.asList(
                "http://localhost:4200",
                "http://172.30.120.90:4200"
            )
        );


        // =====================================================
        // Métodos permitidos
        // =====================================================

        configuration.setAllowedMethods(
            Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )
        );


        // =====================================================
        // Headers permitidos
        //
        // "*" permite Authorization, Content-Type, etc.
        // =====================================================

        configuration.setAllowedHeaders(
            Arrays.asList("*")
        );


        // =====================================================
        // Permitir credenciales.
        //
        // Está permitido porque NO estamos usando "*" en
        // allowedOrigins; tenemos orígenes específicos.
        // =====================================================

        configuration.setAllowCredentials(
            true
        );


        // Opcional, pero útil si necesitas leer Authorization
        // desde una respuesta del backend.
        configuration.setExposedHeaders(
            Arrays.asList(
                "Authorization"
            )
        );


        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
            "/**",
            configuration
        );


        return source;
    }
}