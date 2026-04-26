package com.mraffi.ecommerce_api.security;

import com.mraffi.ecommerce_api.repository.SessionRepository;
import com.mraffi.ecommerce_api.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

   private final JwtService jwtService;
   private final SessionRepository sessionRepository;

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

      String authHeader = request.getHeader("Authorization");

      if(authHeader != null && authHeader.startsWith("Bearer ")){
         String token = authHeader.substring(7);
         try{
            Claims claims = jwtService.validateAccessToken(token);

            String sessionId = claims.get("sessionId", String.class);
            Integer tokenVersionInJwt = claims.get("tokenVersion", Integer.class);

            sessionRepository.findById(sessionId).ifPresentOrElse(session -> {
               if(session.getTokenVersion().equals(tokenVersionInJwt)){
                  request.setAttribute("userId", claims.getSubject());
                  request.setAttribute("sessionId", sessionId);
               } else {
                  System.out.println("Token version mismatch!");
               }
            }, () -> {
               System.out.println("Session not found in DB!");
            });;
         }catch (Exception e){
            System.out.println("JWT Validation failed: " + e.getMessage());
         }
      }

      filterChain.doFilter(request, response);

   }
}
