package com.mraffi.ecommerce_api.dto.response.auth;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {

   private String id;
   private String username;
   private String fullname;
   private String email;
   private Boolean isVerified;

}
