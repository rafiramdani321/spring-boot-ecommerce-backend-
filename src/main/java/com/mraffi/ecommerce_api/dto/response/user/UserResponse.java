package com.mraffi.ecommerce_api.dto.response.user;

import com.mraffi.ecommerce_api.constant.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
   private String id;
   private String username;
   private String fullname;
   private String email;
   private String imageUrl;
   private Boolean isVerified;
   private Enum<Gender> gender;
   private LocalDate dateOfBirth;
}
