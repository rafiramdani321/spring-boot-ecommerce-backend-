package com.mraffi.ecommerce_api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class AccessTokenRequest {

   @NonNull
   private String userId;

   @NonNull
   private String username;

   @NonNull
   private String email;

   @NonNull
   private String role;

   @NonNull
   private Integer tokenVersion;

   @NonNull
   private String sessionId;

   @NonNull
   private String deviceHash;

}
