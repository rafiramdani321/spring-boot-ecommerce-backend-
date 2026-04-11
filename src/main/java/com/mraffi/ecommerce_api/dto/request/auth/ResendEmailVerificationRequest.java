package com.mraffi.ecommerce_api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendEmailVerificationRequest {

   @NotBlank(message = "Email is required")
   @Email(message = "Email is invalid")
   @Size(max = 100, message = "Email must not exceed 100 characters")
   private String email;

}
