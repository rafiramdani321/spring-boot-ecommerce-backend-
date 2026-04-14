package com.mraffi.ecommerce_api.entity;

import com.mraffi.ecommerce_api.constant.AuthProvider;
import com.mraffi.ecommerce_api.constant.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

   private static final String DEFAULT_AVATAR = "https://ui-avatars.com/api/?name=";

   @Id
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

   private Instant createdAt;

   private Instant updatedAt;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
   private Role role;

   @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<Token> tokens = new ArrayList<>();

   @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<Session> sessions = new ArrayList<>();

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

   public void verify(){
      this.isVerified = true;
   }

   public static User createLocalUser(String username, String fullname, String email, String hashedPassword, Role role){
      User user = new User();
      user.username = username;
      user.fullname = fullname;
      user.email = email;
      user.password = hashedPassword;
      user.isVerified = false;
      user.authProvider = AuthProvider.LOCAL;
      user.role = role;
      user.imageUrl = DEFAULT_AVATAR + username;

      return user;
   }
}
