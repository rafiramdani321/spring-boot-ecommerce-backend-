package com.mraffi.ecommerce_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class Role {

   @Id
   private String id;

   @Column(nullable = false, unique = true, length = 100)
   private String name;

   private Instant createdAt;
   private String createdBy;

   private Instant updatedAt;
   private String updatedBy;

   @OneToMany(mappedBy = "role")
   private List<User> users;

   @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<RolePermission> rolePermissions = new ArrayList<>();

   @PrePersist
   public void prePersist(){
      this.id = UUID.randomUUID().toString();
      this.createdAt = Instant.now();
      this.updatedAt = Instant.now();
   }

   @PreUpdate
   public void preUpdate(){
      this.updatedAt = Instant.now();
   }

   public static Role create(String name){
      Role role = new Role();
      role.name = name;

      return role;
   }
}
