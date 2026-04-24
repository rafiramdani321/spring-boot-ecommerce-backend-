package com.mraffi.ecommerce_api.controller;

import com.mraffi.ecommerce_api.dto.WebResponse;
import com.mraffi.ecommerce_api.dto.request.DeviceInfoRequest;
import com.mraffi.ecommerce_api.dto.request.auth.LoginRequest;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.request.auth.ResendEmailVerificationRequest;
import com.mraffi.ecommerce_api.dto.response.auth.LoginResponse;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.service.AuthService;
import com.mraffi.ecommerce_api.service.JwtService;
import com.mraffi.ecommerce_api.util.ClientInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

   private final AuthService authService;
   private final JwtService jwtService;

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

   @PostMapping(
           path = "/resend-email-verification",
           consumes = MediaType.APPLICATION_JSON_VALUE,
           produces = MediaType.APPLICATION_JSON_VALUE
   )
   @ResponseStatus(HttpStatus.CREATED)
   public WebResponse<String> resendEmailVerification(@Valid @RequestBody ResendEmailVerificationRequest request){
      authService.resendEmailVerification(request.getEmail());
      return WebResponse.<String>builder()
              .data("success")
              .message("Resend email verification success")
              .build();
   }

   @PostMapping(
           path = "/login",
           consumes = MediaType.APPLICATION_JSON_VALUE,
           produces = MediaType.APPLICATION_JSON_VALUE
   )
   public WebResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response){
      Map<String, String> clientInfo = ClientInfoUtil.getClientInfo(request);
      String ip = clientInfo.get("ip");
      String userAgent = clientInfo.get("userAgent");

      LoginResponse loginResponse = authService.login(loginRequest, ip, userAgent);

      long maxAge = jwtService.getRefreshTokenExpirationInSeconds();
      ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
              .httpOnly(true)
              .secure(true)
              .path("/")
              .maxAge(maxAge)
              .sameSite("Lax")
              .build();

      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

      return WebResponse.<LoginResponse>builder()
              .data(loginResponse)
              .message("Login success")
              .build();
   }

}
