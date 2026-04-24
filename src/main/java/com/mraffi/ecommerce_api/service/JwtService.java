package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.dto.request.AccessTokenRequest;
import com.mraffi.ecommerce_api.dto.request.RefreshTokenRequest;
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

   @Value("${jwt.access.token.secret}")
   private String accessTokenSecret;

   @Value("${jwt.access.token.expiration}")
   private long accessTokenExpiration;

   @Value("${jwt.refresh.token.secret}")
   private String refreshTokenSecret;

   @Value("${jwt.refresh.token.expiration}")
   private long refreshTokenExpiration;

   private SecretKey getEmailVerificationKey(){
      return Keys.hmacShaKeyFor(emailVerificationSecret.getBytes());
   }

   private SecretKey getAccessTokenKey(){
      return Keys.hmacShaKeyFor(accessTokenSecret.getBytes());
   }

   private SecretKey getRefreshToken(){
      return Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
   }

   public long getRefreshTokenExpirationInSeconds() {
      return refreshTokenExpiration / 1000;
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

   public String generateAccessToken(AccessTokenRequest payload){
      return Jwts.builder()
              .setSubject(payload.getUserId())
              .claim("username", payload.getUsername())
              .claim("email", payload.getEmail())
              .claim("role", payload.getRole())
              .claim("tokenVersion",payload.getTokenVersion())
              .claim("sessionId", payload.getSessionId())
              .claim("deviceHash", payload.getDeviceHash())
              .claim("type", "ACCESS_TOKEN")
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
              .signWith(getAccessTokenKey())
              .compact();
   }

   public String generateRefreshToken(RefreshTokenRequest payload){
      return Jwts.builder()
              .setSubject(payload.getUserId())
              .claim("role", payload.getRole())
              .claim("sessionId", payload.getSessionId())
              .claim("type", "REFRESH_TOKEN")
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
              .signWith(getRefreshToken())
              .compact();
   }

   private Claims parseToken(String token, SecretKey key){
      return Jwts.parserBuilder()
              .setSigningKey(key)
              .build()
              .parseClaimsJws(token)
              .getBody();
   }

   public Claims validateEmailVerificationToken(String token){
      return parseToken(token, getEmailVerificationKey());
   }

   public Claims validateAccessToken(String token){
      return parseToken(token, getAccessTokenKey());
   }

   public Claims validateRefreshToken(String token){
      return parseToken(token, getRefreshToken());
   }
}
