package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
import com.mraffi.ecommerce_api.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

   Optional<Token> findByToken(String token);

   @Modifying
   @Query("UPDATE Token t SET t.tokenStatus = :status WHERE t.user.id = :userId AND t.tokenStatus = 'ACTIVE' AND t.tokenType = :type")
   void invalidateOldTokens(@Param("userId") String userId, @Param("status") TokenStatus status, @Param("type") TokenType type);
}
