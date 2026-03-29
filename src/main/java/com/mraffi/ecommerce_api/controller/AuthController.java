package com.mraffi.ecommerce_api.controller;

import com.mraffi.ecommerce_api.dto.WebResponse;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

   private final AuthService authService;

   @PostMapping(
           path = "/register",
           consumes = MediaType.APPLICATION_JSON_VALUE,
           produces = MediaType.APPLICATION_JSON_VALUE
   )
   @ResponseStatus(HttpStatus.CREATED)
   public WebResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request){
      RegisterResponse registerResponse = authService.register(request);
      return WebResponse.<RegisterResponse>builder()
              .data(registerResponse)
              .message("Registration Success. Please check your email for activation.")
              .build();
   }

   @GetMapping(
           path = "/verify-email",
           produces = MediaType.APPLICATION_JSON_VALUE
   )
   public WebResponse<String> verifyEmail(@RequestParam("token") String token){
      authService.verifyEmail(token);
      return WebResponse.<String>builder()
              .data("success")
              .message("Email verification successful")
              .build();
   }

}
