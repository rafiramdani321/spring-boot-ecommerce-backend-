package com.mraffi.ecommerce_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "rolePermissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_role_permission",
                        columnNames = {"role_id", "permission_id"}
                )
        }
)
@Getter
@NoArgsConstructor
public class RolePermission {

   @Id
   private String id;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
   @OnDelete(action = OnDeleteAction.CASCADE)
   private Role role;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "permission_id",  referencedColumnName = "id", nullable = false)
   @OnDelete(action = OnDeleteAction.CASCADE)
   private Permission permission;

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

   public static RolePermission create(Role role, Permission permission){
      RolePermission rp = new RolePermission();
      rp.role = role;
      rp.permission = permission;
      return rp;
   }

}
