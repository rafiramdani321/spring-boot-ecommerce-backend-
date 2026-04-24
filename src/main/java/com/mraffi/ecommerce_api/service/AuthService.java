package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.dto.request.DeviceInfoRequest;
import com.mraffi.ecommerce_api.dto.request.auth.LoginRequest;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.LoginResponse;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;

public interface AuthService {
   RegisterResponse register(RegisterRequest request);
   void verifyEmail(String tokenValue);
   void resendEmailVerification(String email);
   LoginResponse login(LoginRequest loginRequest, String ip, String userAgent);
}
