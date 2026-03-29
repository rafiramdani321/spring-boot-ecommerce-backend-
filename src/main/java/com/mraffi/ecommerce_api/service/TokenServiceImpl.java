package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

   private final TokenRepository tokenRepository;

   @Override
   @Transactional(propagation = Propagation.REQUIRES_NEW)
   public void updateTokenStatus(Token token, TokenStatus tokenStatus) {
      token.setTokenStatus(tokenStatus);
      tokenRepository.save(token);
   }
}
