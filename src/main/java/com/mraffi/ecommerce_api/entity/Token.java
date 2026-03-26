package com.mraffi.ecommerce_api.entity;

import com.mraffi.ecommerce_api.contant.TokenStatus;
import com.mraffi.ecommerce_api.contant.TokenType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@Getter
@NoArgsConstructor
public class Token {

   @Id
   private String id;

   @Column(nullable = false, unique = true)
   private String token;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
   private User user;

   @Enumerated(EnumType.STRING)
   @Column(name = "token_type", nullable = false)
   private TokenType tokenType;

   @Setter
   @Enumerated(EnumType.STRING)
   @Column(name = "token_status", nullable = false)
   private TokenStatus tokenStatus;

   @Column(name = "expired_at")
   private Instant expiredAt;

   @Column(name = "created_at")
   private Instant createdAt;

   @Column(name = "updated_at")
   private Instant updatedAt;

   @PrePersist
   protected void prePersist(){
      this.id = UUID.randomUUID().toString();
      this.createdAt = Instant.now();
      this.updatedAt = Instant.now();
   }

   @PreUpdate
   protected void preUpdate(){
      this.updatedAt = Instant.now();
   }

   public static Token create(String token, User user, TokenType tokenType, Instant expiredAt){
      Token createToken = new Token();
      createToken.token = token;
      createToken.user = user;
      createToken.tokenType = tokenType;
      createToken.tokenStatus = TokenStatus.ACTIVE;
      createToken.expiredAt = expiredAt;

      return createToken;
   }
}
