package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.entity.Token;

public interface TokenService {
   void updateTokenStatus(Token token, TokenStatus tokenStatus);
}
