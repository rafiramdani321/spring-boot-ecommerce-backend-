package com.mraffi.ecommerce_api.controller;

import com.mraffi.ecommerce_api.dto.WebResponse;
import com.mraffi.ecommerce_api.dto.response.user.UserResponse;
import com.mraffi.ecommerce_api.security.CurrentUser;
import com.mraffi.ecommerce_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

   private final UserService userService;

   @GetMapping(
           path = "/me",
           produces = MediaType.APPLICATION_JSON_VALUE
   )
   public WebResponse<UserResponse> getMe(@CurrentUser String userId) {
      UserResponse response = userService.getMe(userId);
      return WebResponse.<UserResponse>builder()
              .data(response)
              .message("Get me success")
              .build();
   }

}
