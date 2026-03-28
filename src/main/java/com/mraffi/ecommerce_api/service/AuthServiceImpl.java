package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.entity.User;
import com.mraffi.ecommerce_api.exception.ApiException;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import com.mraffi.ecommerce_api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

   private final UserRepository userRepository;
   private final RoleRepository roleRepository;
   private final TokenRepository tokenRepository;

   private final JwtService jwtService;
   private final EmailService emailService;

   private final PasswordEncoder passwordEncoder;

   @Value("${jwt.email.verification.token.expiration}")
   private long emailVerificationTokenExpiration;

   @Transactional
   public RegisterResponse register(RegisterRequest request){
      if(userRepository.existsByUsername(request.getUsername())){
         throw new ApiException(
                 "USERNAME_ALREADY_EXISTS",
                 HttpStatus.CONFLICT,
                 Map.of("username", List.of("Username already exists"))
         );
      }
      if(userRepository.existsByEmail(request.getEmail())){
         throw new ApiException(
                 "EMAIL_ALREADY_REGISTERED",
                 HttpStatus.CONFLICT,
                 Map.of("email", List.of("Email already registered"))
         );
      }
      if(!Objects.equals(request.getPassword(), request.getConfirmPassword())){
         throw new ApiException(
                 "PASSWORDS_DO_NOT_MATCH",
                 HttpStatus.BAD_REQUEST,
                 Map.of("confirmPassword", List.of("Password confirmation does not match"))
         );
      }

      Role role = roleRepository.findByName(RoleName.CUSTOMER.name())
              .orElseThrow(() -> new ApiException(
                      "DEFAULT_ROLE_NOT_FOUND",
                      HttpStatus.BAD_REQUEST,
                      Map.of("global", List.of("Registration Failed. Default role not found"))
              ));

      String hashedPassword = passwordEncoder.encode(request.getPassword());

      User user = User.createLocalUser(
              request.getUsername(),
              request.getFullname(),
              request.getEmail(),
              hashedPassword,
              role
      );

      userRepository.save(user);

      String tokenValue = jwtService.generateEmailVerificationToken(user.getId());
      Instant tokenExp = Instant.now().plusMillis(emailVerificationTokenExpiration);

      Token token = Token.create(
              tokenValue, user, TokenType.EMAIL_ACTIVATION, tokenExp
      );
      tokenRepository.save(token);

      emailService.sendVerificationEmail(user.getEmail(), tokenValue);

      return RegisterResponse.builder()
              .id(user.getId())
              .username(user.getUsername())
              .fullname(user.getFullname())
              .email(user.getEmail())
              .isVerified(user.getIsVerified())
              .build();
   }

   @Transactional
   public void verifyEmail(String tokenValue){
      Token token = tokenRepository.findByToken(tokenValue)
              .orElseThrow(() -> new ApiException(
                      "TOKEN_NOT_FOUND",
                      HttpStatus.NOT_FOUND,
                      Map.of("token", List.of("Invalid token"))
              ));

         if(token.getTokenStatus() != TokenStatus.ACTIVE){
            throw new ApiException(
                    "TOKEN_ALREADY_USED",
                    HttpStatus.BAD_REQUEST,
                    Map.of("token", List.of("Token already used or invalid"))
            );
         }

         if(token.getExpiredAt().isBefore(Instant.now())){
            throw new ApiException(
                    "TOKEN_EXPIRED",
                    HttpStatus.BAD_REQUEST,
                    Map.of("token", List.of("Token has expired"))
            );
         }

      Claims claims;
      try {
         claims = jwtService.validateToken(tokenValue);
      } catch (ExpiredJwtException e){
         throw new ApiException(
                 "TOKEN_EXPIRED",
                 HttpStatus.BAD_REQUEST,
                 Map.of("token", List.of("Token has expired"))
         );
      } catch (UnsupportedJwtException | MalformedJwtException e) {
         throw new ApiException(
                 "INVALID_TOKEN",
                 HttpStatus.BAD_REQUEST,
                 Map.of("token", List.of("Invalid token format"))
         );
      } catch (SignatureException e) {
         throw new ApiException(
                 "INVALID_SIGNATURE",
                 HttpStatus.BAD_REQUEST,
                 Map.of("token", List.of("Invalid token signature"))
         );

      } catch (Exception e) {
         throw new ApiException(
                 "INVALID_TOKEN",
                 HttpStatus.BAD_REQUEST,
                 Map.of("token", List.of("Token validation failed"))
         );
      }

      String userId = claims.getSubject();

      User user = userRepository.findById(userId)
              .orElseThrow(() -> new ApiException(
                      "USER_NOT_FOUND",
                      HttpStatus.NOT_FOUND,
                      Map.of("global", List.of("User not found"))
              ));

      user.verify();
      token.setTokenStatus(TokenStatus.USED);

      userRepository.save(user);
      tokenRepository.save(token);
   }

}
