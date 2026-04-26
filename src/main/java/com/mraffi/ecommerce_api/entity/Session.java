package com.mraffi.ecommerce_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity()
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_session_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_user_device",
                        columnNames = {"user_id", "device_hash"}
                )
        }
)
@Getter
@NoArgsConstructor
public class Session {

   @Id
   private String id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
   @OnDelete(action = OnDeleteAction.CASCADE)
   private User user;

   @Column(name = "device_hash", nullable = false)
   private String deviceHash;

   @Column(name = "user_agent")
   @Setter
   private String userAgent;

   @Column(name = "ip_address")
   @Setter
   private String ipAddress;

   @Column(name = "token_version", nullable = false)
   private Integer tokenVersion;

   @Column(name = "created_at")
   private Instant createdAt;

   @Column(name = "updated_at")
   private Instant updatedAt;

   @PrePersist
   protected void prePersist(){
      this.id = UUID.randomUUID().toString();
      this.createdAt = Instant.now();
      this.updatedAt = Instant.now();

      if(this.tokenVersion == null){
         this.tokenVersion = 1;
      }
   }

   @PreUpdate
   protected void preUpdate(){
      this.updatedAt = Instant.now();
   }

   public static Session create(User user, String userAgent, String ipAddress, String deviceHash){
      Session createSession = new Session();
      createSession.user = user;
      createSession.userAgent = userAgent;
      createSession.ipAddress = ipAddress;
      createSession.deviceHash = deviceHash;

      return createSession;
   }

   public void incrementTokenVersion(){
      this.tokenVersion++;
   }
}
