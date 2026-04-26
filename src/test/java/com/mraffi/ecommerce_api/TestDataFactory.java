package com.mraffi.ecommerce_api;

import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.entity.User;

import java.time.Instant;

import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TestDataFactory {

   public static User createTestUser(Role role, String id, boolean isVerified) {
      User user = User.createLocalUser("testuser", "Test User", "testuser@example.com", "123@Password456", role);
      setField(user, "id", id);
      if(isVerified) user.verify();
      return user;
   }

   public static Role createRole(String name){
      return Role.create(name);
   }

   public static Token createToken(User user, TokenType tokenType, TokenStatus tokenStatus){
      Token token = Token.create("valid.token", user, tokenType, Instant.now().plusSeconds(900));
      token.setTokenStatus(tokenStatus);
      return token;
   }

}
