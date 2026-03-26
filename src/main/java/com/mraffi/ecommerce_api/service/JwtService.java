package com.mraffi.ecommerce_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

   @Value("${jwt.email.verification.token.secret}")
   private String emailVerificationSecret;

   @Value("${jwt.email.verification.token.expiration}")
   private long emailVerificationExpiration;

   private SecretKey getEmailVerificationKey(){
      return Keys.hmacShaKeyFor(emailVerificationSecret.getBytes());
   }

   public String generateEmailVerificationToken(String userId){
      return Jwts.builder()
              .setSubject(userId)
              .claim("type", "EMAIL_VERIFICATION")
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + emailVerificationExpiration))
              .signWith(getEmailVerificationKey())
              .compact();
   }

   public Claims validateToken(String token){
      return getClaims(token);
   }

   private Claims getClaims(String token){
      return Jwts.parserBuilder()
              .setSigningKey(getEmailVerificationKey())
              .build()
              .parseClaimsJws(token)
              .getBody();
   }

}
