package com.mraffi.ecommerce_api.repository;

import com.mraffi.ecommerce_api.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

   Optional<Token> findByToken(String token);
}
