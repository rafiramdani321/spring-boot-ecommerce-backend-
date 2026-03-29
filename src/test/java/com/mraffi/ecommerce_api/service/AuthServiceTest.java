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
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

   @Mock
   private UserRepository userRepository;

   @Mock
   private RoleRepository roleRepository;

   @Mock
   private TokenRepository tokenRepository;

   @Mock
   private JwtService jwtService;

   @Mock
   private EmailService emailService;

   @Mock
   private TokenService tokenService;

   @Mock
   private PasswordEncoder passwordEncoder;

   @Mock
   private Claims claims;
   @Mock
   private ExpiredJwtException expiredJwtException;
   @Mock
   private UnsupportedJwtException unsupportedJwtException;
   @Mock
   private MalformedJwtException malformedJwtException;
   @Mock
   private SignatureException signatureException;

   @InjectMocks
   private AuthServiceImpl authService;

   private RegisterRequest registerRequest;

   private Token token;
   private Role role;
   private User user;

   @BeforeEach
   void setUp(){
      registerRequest = RegisterRequest.builder()
              .username("newuser")
              .fullname("New User")
              .email("new@example.com")
              .password("123@Password456")
              .confirmPassword("123@Password456")
              .build();

      setField(authService, "emailVerificationTokenExpiration", 900000L);

      role = Role.create("CUSTOMER");

      user = User.createLocalUser(
              "testuser",
              "Test User",
              "testuser@example.com",
              "hashedPassword",
              role
      );
      setField(user, "id", "UUID-USER-TEST");

      token = Token.create(
              "valid.jwt.token",
              user,
              TokenType.EMAIL_ACTIVATION,
              Instant.now().plusMillis(900000L)
      );
   }

   @Test
   void register_success(){
      when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER.name())).thenReturn(Optional.of(role));
      when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");

      when(userRepository.save(any(User.class))).thenAnswer(
              invocation -> {
                 User user = invocation.getArgument(0);
                 setField(user, "id", "UUID-TEST-123");
                 return user;
              }
      );

      when(jwtService.generateEmailVerificationToken(any())).thenReturn("mockToken");

      RegisterResponse response = authService.register(registerRequest);

      assertNotNull(response);
      assertEquals(registerRequest.getUsername(), response.getUsername());
      assertEquals(registerRequest.getFullname(), response.getFullname());
      assertEquals(registerRequest.getEmail(), response.getEmail());
      assertFalse(response.getIsVerified());

      verify(userRepository).save(any(User.class));
      verify(tokenRepository).save(any(Token.class));
      verify(emailService).sendVerificationEmail(response.getEmail(), "mockToken");
   }

   @Test
   void register_usernameAlreadyExists(){
      when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());

      assertEquals("USERNAME_ALREADY_EXISTS", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("username"));
      assertTrue(ex.getErrors().get("username").contains("Username already exists"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_emailAlreadyExists(){
      when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());

      assertEquals("EMAIL_ALREADY_REGISTERED", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("email"));
      assertTrue(ex.getErrors().get("email").contains("Email already registered"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_passwordMismatch(){
      registerRequest.setConfirmPassword("wrongConfirmPassword");
      when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());

      assertEquals("PASSWORDS_DO_NOT_MATCH", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("confirmPassword"));
      assertTrue(ex.getErrors().get("confirmPassword").contains("Password confirmation does not match"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_roleNotFound(){
      when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER.name())).thenReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());

      assertEquals("DEFAULT_ROLE_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("global"));
      assertTrue(ex.getErrors().get("global").contains("Registration Failed. Default role not found."));

      verify(userRepository, never()).save(any());
   }

   @Test
   void verifyEmail_success(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
      when(jwtService.validateToken(token.getToken())).thenReturn(claims);
      when(claims.getSubject()).thenReturn(user.getId());
      when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

      authService.verifyEmail(token.getToken());

      assertTrue(user.getIsVerified());
      verify(userRepository, times(1)).save(user);

      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.USED);
   }

   @Test
   void verifyEmail_failed_tokenNotFound(){
      when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail("invalid-token"));

      assertNotNull(ex.getErrors());

      assertEquals("TOKEN_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("Invalid token"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_tokenAlreadyUsed(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
      token.setTokenStatus(TokenStatus.USED);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("TOKEN_ALREADY_USED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("This link has already been used. Please login."));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_expiredDb(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
      setField(token, "expiredAt", Instant.now().minusSeconds(10));

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("TOKEN_EXPIRED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("This link has expired. Please request a new verification link."));
      assertTrue(ex.getErrors().containsKey("email"));
      assertTrue(ex.getErrors().get("email").contains("testuser@example.com"));

      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.EXPIRED);
      verify(userRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_jwtExpired(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

      when(jwtService.validateToken(token.getToken()))
              .thenThrow(ExpiredJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("TOKEN_EXPIRED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("This link has expired. Please request a new verification link."));
      assertTrue(ex.getErrors().containsKey("email"));
      assertTrue(ex.getErrors().get("email").contains("testuser@example.com"));

      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.EXPIRED);
      verify(userRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_jwtUnsupported(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

      when(jwtService.validateToken(token.getToken()))
              .thenThrow(UnsupportedJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("INVALID_TOKEN", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("Invalid token format"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_jwtMalformed(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

      when(jwtService.validateToken(token.getToken()))
              .thenThrow(MalformedJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("INVALID_TOKEN", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("Invalid token format"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_jwtInvalidSignature(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

      when(jwtService.validateToken(token.getToken()))
              .thenThrow(SignatureException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("INVALID_SIGNATURE", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("token"));
      assertTrue(ex.getErrors().get("token").contains("Invalid token signature"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   void verifyEmail_failed_userNotFound(){
      when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
      when(jwtService.validateToken(token.getToken())).thenReturn(claims);

      String userIdFromClaims = "unknown-id";
      when(claims.getSubject()).thenReturn(userIdFromClaims);

      when(userRepository.findById(userIdFromClaims)).thenReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());

      assertEquals("USER_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("global"));
      assertTrue(ex.getErrors().get("global").contains("User not found"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }
}