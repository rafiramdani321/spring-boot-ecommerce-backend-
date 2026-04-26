package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
import com.mraffi.ecommerce_api.dto.request.AccessTokenRequest;
import com.mraffi.ecommerce_api.dto.request.DeviceInfoRequest;
import com.mraffi.ecommerce_api.dto.request.RefreshTokenRequest;
import com.mraffi.ecommerce_api.dto.request.auth.LoginRequest;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.LoginResponse;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.Session;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.entity.User;
import com.mraffi.ecommerce_api.exception.ApiException;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import com.mraffi.ecommerce_api.repository.SessionRepository;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import com.mraffi.ecommerce_api.repository.UserRepository;
import com.mraffi.ecommerce_api.util.ClientInfoUtil;
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
   private final SessionRepository sessionRepository;

   private final TokenService tokenService;
   private final JwtService jwtService;
   private final EmailService emailService;

   private final PasswordEncoder passwordEncoder;

   @Value("${jwt.email.verification.token.expiration}")
   private long emailVerificationTokenExpiration;

   @Override
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
                      Map.of("global", List.of("Registration Failed. Default role not found."))
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

   @Override
   @Transactional
   public void verifyEmail(String tokenValue){
      Token token = tokenRepository.findByToken(tokenValue)
              .orElseThrow(() -> new ApiException(
                      "TOKEN_NOT_FOUND",
                      HttpStatus.NOT_FOUND,
                      Map.of("token", List.of("Invalid token"))
              ));

      String userEmail = token.getUser().getEmail();
      String expiredMsg = "This link has expired. Please request a new verification link.";
      Map<String, List<String>> expiredData = Map.of(
              "token", List.of(expiredMsg),
              "email", List.of(userEmail)
      );

      if(token.getTokenStatus() == TokenStatus.USED){
         throw new ApiException(
                 "TOKEN_ALREADY_USED",
                 HttpStatus.BAD_REQUEST,
                 Map.of("token", List.of("This link has already been used. Please login."))
         );
      }

      if(token.getTokenStatus() == TokenStatus.EXPIRED){
         throw new ApiException(
                 "TOKEN_EXPIRED",
                 HttpStatus.BAD_REQUEST,
                 expiredData
         );
      }


      if(token.getExpiredAt().isBefore(Instant.now())){
         tokenService.updateTokenStatus(token, TokenStatus.EXPIRED);
         throw new ApiException(
                 "TOKEN_EXPIRED",
                 HttpStatus.BAD_REQUEST,
                 expiredData
         );
      }

      Claims claims;
      try {
         claims = jwtService.validateEmailVerificationToken(tokenValue);
      } catch (ExpiredJwtException e){
         tokenService.updateTokenStatus(token, TokenStatus.EXPIRED);
         throw new ApiException(
                 "TOKEN_EXPIRED",
                 HttpStatus.BAD_REQUEST,
                 expiredData
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
      userRepository.save(user);

      tokenService.updateTokenStatus(token, TokenStatus.USED);
   }

   private User getByEmail(String email){
      return userRepository.findByEmail(email).orElseThrow(
              () -> new ApiException(
                      "USER_NOT_FOUND",
                      HttpStatus.NOT_FOUND,
                      Map.of("global", List.of("User not found"))
              )
      );
   }

   @Override
   @Transactional
   public void resendEmailVerification(String email){
      User user = getByEmail(email);

      if(user.getIsVerified()){
         throw new ApiException(
                 "USER_HAS_VERIFIED",
                 HttpStatus.BAD_REQUEST,
                 Map.of("global", List.of("User has verified"))
         );
      }

      tokenRepository.invalidateOldTokens(user.getId(), TokenStatus.EXPIRED, TokenType.EMAIL_ACTIVATION);

      String tokenValue = jwtService.generateEmailVerificationToken(user.getId());
      Instant tokenExp = Instant.now().plusMillis(emailVerificationTokenExpiration);

      Token token = Token.create(
              tokenValue, user, TokenType.EMAIL_ACTIVATION, tokenExp
      );
      tokenRepository.save(token);

      emailService.sendVerificationEmail(user.getEmail(), tokenValue);
   }

   @Override
   @Transactional
   public LoginResponse login(LoginRequest loginRequest, String ip, String userAgent){
      User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(
              () -> new ApiException(
                      "LOGIN_FAILED",
                      HttpStatus.BAD_REQUEST,
                      Map.of("global", List.of("Email or Password incorrect"))
              )
      );

      boolean isValidPassword = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
      if(!isValidPassword){
         throw new ApiException(
                 "LOGIN_FAILED",
                 HttpStatus.BAD_REQUEST,
                 Map.of("global",  List.of("Email or Password incorrect"))
         );
      }

      if(!user.getIsVerified()){
         throw new ApiException(
                 "LOGIN_FAILED",
                 HttpStatus.BAD_REQUEST,
                 Map.of(
                         "global", List.of("Your email has not been activated. please check your email or you can request a new activation link."),
                         "email", List.of(user.getEmail())
                 )
         );
      }

      String deviceHash = ClientInfoUtil.generateDeviceHash(
              ip, userAgent
      );

      Session session = sessionRepository.findByUserIdAndDeviceHash(user.getId(), deviceHash)
              .map(existingSession -> {
                 existingSession.setIpAddress(ip);
                 existingSession.setUserAgent(userAgent);
                 existingSession.incrementTokenVersion();

                 return existingSession;
              })
              .orElseGet(() -> Session.create(user, userAgent, ip, deviceHash));

      sessionRepository.save(session);

      AccessTokenRequest payloadAccessToken = AccessTokenRequest.builder()
              .userId(user.getId())
              .username(user.getUsername())
              .email(user.getEmail())
              .role(user.getRole().getName())
              .tokenVersion(session.getTokenVersion())
              .sessionId(session.getId())
              .deviceHash(deviceHash)
              .build();

      RefreshTokenRequest payloadRefreshToken = RefreshTokenRequest.builder()
              .userId(user.getId())
              .role(user.getRole().getName())
              .sessionId(session.getId())
              .build();

      String accessToken = jwtService.generateAccessToken(payloadAccessToken);
      String refreshToken = jwtService.generateRefreshToken(payloadRefreshToken);

      return LoginResponse.builder()
              .username(user.getUsername())
              .email(user.getEmail())
              .accessToken(accessToken)
              .refreshToken(refreshToken)
              .build();
   }

   @Override
   @Transactional
   public LoginResponse refreshToken(String refreshToken){
      Claims claims = jwtService.validateRefreshToken(refreshToken);

      String sessionId = claims.get("sessionId", String.class);
      String userId = claims.getSubject();

      User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(
              "USER_NOT_FOUND",
              HttpStatus.NOT_FOUND,
              Map.of("global", List.of("User not found"))
      ));

      Session session = sessionRepository.findById(sessionId)
              .filter(s -> s.getUser().getId().equals(user.getId()))
              .orElseThrow(() -> new ApiException(
                      "UNAUTHORIZED",
                      HttpStatus.UNAUTHORIZED,
                      Map.of("global", List.of("Unauthorized"))
              ));

      session.incrementTokenVersion();
      sessionRepository.save(session);

      AccessTokenRequest payloadAccessToken = AccessTokenRequest.builder()
              .userId(user.getId())
              .username(user.getUsername())
              .email(user.getEmail())
              .role(user.getRole().getName())
              .tokenVersion(session.getTokenVersion())
              .sessionId(session.getId())
              .deviceHash(session.getDeviceHash())
              .build();

      String newAccessToken = jwtService.generateAccessToken(payloadAccessToken);

      return LoginResponse.builder()
              .username(payloadAccessToken.getUsername())
              .email(payloadAccessToken.getEmail())
              .accessToken(newAccessToken)
              .build();
   }
}
