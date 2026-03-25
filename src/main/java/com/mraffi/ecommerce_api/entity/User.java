package com.mraffi.ecommerce_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

   @Id
   @Setter(AccessLevel.NONE)
   private String id;

   @Column(nullable = false, unique = true, length = 50)
   private String username;

   @Column(length = 100)
   private String fullname;

   @Column(nullable = false, unique = true, length = 100)
   private String email;

   @Column(nullable = false)
   private String password;

   @Column(name = "image_url")
   private String imageUrl;

   @Column(name = "is_verified")
   private Boolean isVerified = false;

   @Column(name = "auth_provider")
   @Enumerated(EnumType.STRING)
   private AuthProvider authProvider;

   @Enumerated(EnumType.STRING)
   private Gender gender;

   @Column(name = "date_of_birth")
   private LocalDate dateOfBirth;

   @Setter(AccessLevel.NONE)
   private Instant createdAt;

   @Setter(AccessLevel.NONE)
   private Instant updatedAt;

   @ManyToOne
   @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
   private Role role;

   @PrePersist
   protected void prePersist(){
      this.id = UUID.randomUUID().toString();
      this.createdAt = Instant.now();
      this.updatedAt = Instant.now();

      if(this.isVerified == null) this.isVerified = false;
      if(this.authProvider == null) this.authProvider = AuthProvider.LOCAL;
   }

   @PreUpdate
   protected void preUpdate(){
      this.updatedAt = Instant.now();
   }

}
