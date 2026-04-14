package com.mraffi.ecommerce_api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {

   @NotBlank(message = "Email is required")
   @Email(message = "Email is invalid")
   private String email;

   @NotBlank(message = "Password is required")
   private String password;

}
