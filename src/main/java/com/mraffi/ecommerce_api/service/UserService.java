package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.dto.response.user.UserResponse;

public interface UserService {
   UserResponse getMe(String userId);
}
