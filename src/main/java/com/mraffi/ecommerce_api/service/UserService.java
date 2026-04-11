package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.dto.response.user.UserResponse;
import com.mraffi.ecommerce_api.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

   public UserResponse toUserResponse(User user){
      return UserResponse.builder()
              .id(user.getId())
              .username(user.getUsername())
              .fullname(user.getFullname())
              .email(user.getEmail())
              .imageUrl(user.getImageUrl())
              .dateOfBirth(user.getDateOfBirth())
              .gender(user.getGender())
              .isVerified(user.getIsVerified())
              .build();
   }

}
