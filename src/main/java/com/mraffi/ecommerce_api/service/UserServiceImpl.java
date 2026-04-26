package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.dto.response.user.UserResponse;
import com.mraffi.ecommerce_api.entity.User;
import com.mraffi.ecommerce_api.exception.ApiException;
import com.mraffi.ecommerce_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

   private final UserRepository userRepository;

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

   public UserResponse getMe(String userId){
      User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(
              "USER_NOT_FOUND",
              HttpStatus.NOT_FOUND,
              Map.of("global", List.of("User not found"))
      ));

      return toUserResponse(user);
   }

}
