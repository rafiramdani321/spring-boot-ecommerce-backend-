package com.mraffi.ecommerce_api.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class RefreshTokenRequest {

   @NonNull
   private String userId;

   @NonNull
   private String role;

   @NonNull
   private String sessionId;

}
