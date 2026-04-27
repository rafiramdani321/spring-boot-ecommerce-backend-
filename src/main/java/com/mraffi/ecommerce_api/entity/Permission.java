package com.mraffi.ecommerce_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor
public class Permission {

   @Id
   private String id;

   @Column(nullable = false)
   private String name;

   private String description;
   private String module;

   @Column(name = "created_at")
   private Instant createdAt;

   @Column(name = "updated_at")
   private Instant updatedAt;

   @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<RolePermission> rolePermissions = new ArrayList<>();

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

   public static Permission create(String name, String description, String module){
      Permission p = new Permission();
      p.name = name;
      p.description = description;
      p.module = module;
      return p;
   }

}
