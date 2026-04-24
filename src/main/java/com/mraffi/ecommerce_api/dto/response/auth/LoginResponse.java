package com.mraffi.ecommerce_api.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {

   private String username;
   private String email;
   private String accessToken;
   private String refreshToken;

}
