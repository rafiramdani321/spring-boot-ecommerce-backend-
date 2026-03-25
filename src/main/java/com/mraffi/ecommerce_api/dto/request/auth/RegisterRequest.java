package com.mraffi.ecommerce_api.dto.request.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {

   @NotBlank(message = "Username is required")
   @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
   private String username;

   @Size(max = 100, message = "Fullname must not exceed 100 characters")
   private String fullname;

   @NotBlank(message = "Email is required")
   @Email(message = "Email is invalid")
   @Size(max = 100, message = "Email must not exceed 100 characters")
   private String email;

   @NotBlank(message = "Password is required")
   @Size(min = 8, message = "Password must be at least 8 characters")
   @Pattern(
           regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).+$",
           message = "Password must contain at least 1 uppercase and 1 special character"
   )
   private String password;

   @NotBlank(message = "Confirm password is required")
   private String confirmPassword;

}
