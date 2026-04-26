package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.TestDataFactory;
import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
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
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.swing.text.html.Option;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

   @Mock private UserRepository userRepository;
   @Mock private RoleRepository roleRepository;
   @Mock private TokenRepository tokenRepository;
   @Mock private SessionRepository sessionRepository;
   @Mock private JwtService jwtService;
   @Mock private EmailService emailService;
   @Mock private TokenService tokenService;
   @Mock private PasswordEncoder passwordEncoder;
   @Mock private Claims claims;

   @InjectMocks
   private AuthServiceImpl authService;

   private Token token;
   private Role role;
   private User user;
   private RegisterRequest registerRequest;
   private LoginRequest loginRequest;

   @BeforeEach
   void setUp(){
      setField(authService, "emailVerificationTokenExpiration", 900000L);

      role = TestDataFactory.createRole(RoleName.CUSTOMER.name());
      user = TestDataFactory.createTestUser(role, "USER-UUID", false);
      token = TestDataFactory.createToken(user, TokenType.EMAIL_ACTIVATION, TokenStatus.ACTIVE);

      registerRequest = RegisterRequest.builder()
              .username("rafi").fullname("rafi ramdani").email("rafi@example.com").password("123@Password456").confirmPassword("123@Password456").build();


      loginRequest = LoginRequest.builder()
              .email("testuser@example.com")
              .password("123@Password456")
              .build();

//      role = TestDataFactory.createRole(RoleName.CUSTOMER.name());
//      user = TestDataFactory.createTestUser(role, "USER-UUID", false);
//      token = TestDataFactory.createToken(user, TokenType.EMAIL_ACTIVATION, TokenStatus.ACTIVE);
//      registerRequest = RegisterRequest.builder()
//              .username("newuser")
//              .fullname("New User")
//              .email("new@example.com")
//              .password("123@Password456")
//              .confirmPassword("123@Password456")
//              .build();
//
//      setField(authService, "emailVerificationTokenExpiration", 900000L);
//
//      role = Role.create("CUSTOMER");
//
//      user = User.createLocalUser(
//              "testuser",
//              "Test User",
//              "testuser@example.com",
//              "hashedPassword",
//              role
//      );
//      setField(user, "id", "UUID-USER-TEST");
//
//      token = Token.create(
//              "valid.jwt.token",
//              user,
//              TokenType.EMAIL_ACTIVATION,
//              Instant.now().plusMillis(900000L)
//      );
//
//      loginRequest = LoginRequest.builder()
//              .email("example@mail.com")
//              .password("123Password456")
//              .build();
   }

//   --- REGISTER SCENARIOS ---

   @Test
   @DisplayName("Register: Should success register and send email")
   void register_success(){
      given(userRepository.existsByUsername(anyString())).willReturn(false);
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(roleRepository.findByName(anyString())).willReturn(Optional.of(role));
      given(passwordEncoder.encode(anyString())).willReturn("encoded");
      given(userRepository.save(any(User.class))).willReturn(user);
      given(jwtService.generateEmailVerificationToken(any())).willReturn("mockToken");

      RegisterResponse response = authService.register(registerRequest);

      assertAll("Register Validations",
              () -> assertNotNull(response),
              () -> verify(emailService).sendVerificationEmail(eq(registerRequest.getEmail()), eq("mockToken")),
              () -> verify(tokenRepository).save(any(Token.class))
      );
   }

   @Test
   @DisplayName("Register: Should throw exception when username already exists")
   void register_usernameAlreadyExists(){
      given(userRepository.existsByUsername(anyString())).willReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());
      assertEquals("USERNAME_ALREADY_EXISTS", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().get("username").contains("Username already exists"));

      verify(userRepository, never()).save(any());
   }

   @Test
   @DisplayName("Register: Should throw exception when email already exists")
   void register_emailAlreadyExists(){
      given(userRepository.existsByUsername(anyString())).willReturn(false);
      given(userRepository.existsByEmail(anyString())).willReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());
      assertEquals("EMAIL_ALREADY_REGISTERED", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().get("email").contains("Email already registered"));

      verify(userRepository, never()).save(any());
   }

   @Test
   @DisplayName("Register: Should throw exception when confirm password does not match")
   void register_passwordMismatch(){
      registerRequest.setConfirmPassword("wrongConfirmPassword");
      given(userRepository.existsByUsername(anyString())).willReturn(false);
      given(userRepository.existsByEmail(anyString())).willReturn(false);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());
      assertEquals("PASSWORDS_DO_NOT_MATCH", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("confirmPassword").contains("Password confirmation does not match"));

      verify(userRepository, never()).save(any());
   }

   @Test
   @DisplayName("Register: Should throw exception when role not found")
   void register_roleNotFound(){
      given(userRepository.existsByUsername(anyString())).willReturn(false);
      given(userRepository.existsByEmail(anyString())).willReturn(false);
      given(roleRepository.findByName(anyString())).willReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

      assertNotNull(ex.getErrors());
      assertEquals("DEFAULT_ROLE_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("global").contains("Registration Failed. Default role not found."));

      verify(userRepository, never()).save(any());
   }

   //   --- VERIFY EMAIL SCENARIOS ---

   @Test
   @DisplayName("Verify Email: Should success verify email")
   void verifyEmail_success(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));
      given(jwtService.validateEmailVerificationToken(anyString())).willReturn(claims);
      given(claims.getSubject()).willReturn("USER-UUID");
      given(userRepository.findById(anyString())).willReturn(Optional.of(user));

      authService.verifyEmail(anyString());

      assertTrue(user.getIsVerified());
      verify(userRepository, times(1)).save(user);
      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.USED);
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when token not found")
   void verifyEmail_failed_tokenNotFound(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("TOKEN_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("Invalid token"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when token already used")
   void verifyEmail_failed_tokenAlreadyUsed(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));
      token.setTokenStatus(TokenStatus.USED);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());

      assertEquals("TOKEN_ALREADY_USED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("This link has already been used. Please login."));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when token expired DB")
   void verifyEmail_failed_expiredDb(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));
      setField(token, "expiredAt", Instant.now().minusSeconds(10));

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(token.getToken()));

      assertNotNull(ex.getErrors());
      assertEquals("TOKEN_EXPIRED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("This link has expired. Please request a new verification link."));
      assertTrue(ex.getErrors().get("email").contains("testuser@example.com"));

      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.EXPIRED);
      verify(userRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when JWT Expired")
   void verifyEmail_failed_jwtExpired(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));
      given(jwtService.validateEmailVerificationToken(anyString())).willThrow(ExpiredJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("TOKEN_EXPIRED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("This link has expired. Please request a new verification link."));
      assertTrue(ex.getErrors().get("email").contains("testuser@example.com"));

      verify(tokenService, times(1)).updateTokenStatus(token, TokenStatus.EXPIRED);
      verify(userRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when JWT Unsupported")
   void verifyEmail_failed_jwtUnsupported(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));

      given(jwtService.validateEmailVerificationToken(anyString())).willThrow(UnsupportedJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("INVALID_TOKEN", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("Invalid token format"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when JWT Malformed")
   void verifyEmail_failed_jwtMalformed(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));

      given(jwtService.validateEmailVerificationToken(anyString())).willThrow(MalformedJwtException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("INVALID_TOKEN", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("Invalid token format"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when JWT Invalid Signature")
   void verifyEmail_failed_jwtInvalidSignature(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));

      given(jwtService.validateEmailVerificationToken(any())).willThrow(SignatureException.class);

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("INVALID_SIGNATURE", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("token").contains("Invalid token signature"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   @Test
   @DisplayName("Verify Email: Should throw exception when user not found")
   void verifyEmail_failed_userNotFound(){
      given(tokenRepository.findByToken(anyString())).willReturn(Optional.of(token));
      given(jwtService.validateEmailVerificationToken(anyString())).willReturn(claims);

      String userIdFromClaims = "unknown-id";
      given(claims.getSubject()).willReturn(userIdFromClaims);
      given(userRepository.findById(userIdFromClaims)).willReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.verifyEmail(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("USER_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
      assertTrue(ex.getErrors().get("global").contains("User not found"));

      verify(userRepository, never()).save(any());
      verify(tokenRepository, never()).save(any());
   }

   //   --- RESEND EMAIL SCENARIOS ---

   @Test
   @DisplayName("Resend Email: should throw exception when user not found")
   void resendEmailVerification_failed_userNotFound(){
      given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.resendEmailVerification(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("USER_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
      assertTrue(ex.getErrors().get("global").contains("User not found"));

      verifyNoInteractions(tokenRepository, jwtService, emailService);
   }

   @Test
   @DisplayName("Resend Email: should throw exception when user has verified")
   void resendEmailVerification_failed_userHasVerified(){
      user.verify();
      given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

      ApiException ex = assertThrows(ApiException.class, () -> authService.resendEmailVerification(anyString()));

      assertNotNull(ex.getErrors());
      assertEquals("USER_HAS_VERIFIED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("global").contains("User has verified"));

      verify(tokenRepository, never()).save(any());
      verify(emailService, never()).sendVerificationEmail(any(), any());
   }

   @Test
   @DisplayName("Resend Email: should success resend email")
   void resendEmailVerification_success(){
      String newTokenValue = "new.valid.jwt.token";

      given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
      given(jwtService.generateEmailVerificationToken(anyString())).willReturn(newTokenValue);

      authService.resendEmailVerification(anyString());

      verify(tokenRepository, times(1)).invalidateOldTokens(
              user.getId(),
              TokenStatus.EXPIRED,
              TokenType.EMAIL_ACTIVATION
      );

      verify(tokenRepository, times(1)).save(any(Token.class));
      verify(emailService, times(1)).sendVerificationEmail(user.getEmail(), newTokenValue);
   }

   //   --- LOGIN SCENARIOS ---

   @Test
   @DisplayName("Login: should success login")
   void login_success() {
      user.verify();
      given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
      given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
      given(sessionRepository.findByUserIdAndDeviceHash(anyString(), anyString())).willReturn(Optional.empty());
      given(jwtService.generateAccessToken(any())).willReturn("access");
      given(jwtService.generateRefreshToken(any())).willReturn("refresh");

      LoginResponse resp = authService.login(loginRequest, "127.0.0.1", "Chrome");

      assertAll("Login Validations",
              () -> assertEquals("access", resp.getAccessToken()),
              () -> verify(sessionRepository).save(any(Session.class))
      );
   }

   @Test
   void login_failed_emailIncorrect(){
      when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.login(loginRequest, "127.0.0.1", "chrome/1.1"));

      assertNotNull(ex.getErrors());

      assertEquals("LOGIN_FAILED", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().get("global").contains("Email or Password incorrect"));

      verify(sessionRepository, never()).save(any());
   }
}