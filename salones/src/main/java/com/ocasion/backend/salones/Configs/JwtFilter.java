package com.ocasion.backend.salones.Configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Aquí ocurre la magia: si la firma no coincide o expiró, lanzará una excepción
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Extraemos la información (Asegúrate de que tu MS de usuarios guarde estos datos en el token)
                String userId = claims.getSubject(); // O el claim donde guardes el id_user
                String role = claims.get("rol", String.class); 

                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}