package com.mraffi.ecommerce_api.service;

import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.entity.User;
import com.mraffi.ecommerce_api.exception.ApiException;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import com.mraffi.ecommerce_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

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
   private PasswordEncoder passwordEncoder;

   @InjectMocks
   private AuthServiceImpl authService;

   private RegisterRequest request;
   private Role role;

   @BeforeEach
   void setUp(){
      request = RegisterRequest.builder()
              .username("testuser")
              .fullname("Test User")
              .email("test@example.com")
              .password("123@Password456")
              .confirmPassword("123@Password456")
              .build();

      role = Role.create("CUSTOMER");

      setField(
              authService,
              "emailVerificationTokenExpiration",
              900000L
      );
   }

   @Test
   void register_success(){
      when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER.name())).thenReturn(Optional.of(role));
      when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");

      when(userRepository.save(any(User.class))).thenAnswer(
              invocation -> {
                 User user = invocation.getArgument(0);
                 setField(user, "id", "UUID-TEST-123");
                 return user;
              }
      );

      when(jwtService.generateEmailVerificationToken(any())).thenReturn("mockToken");

      RegisterResponse response = authService.register(request);

      assertNotNull(response);
      assertEquals("testuser", response.getUsername());
      assertEquals("Test User", response.getFullname());
      assertEquals("test@example.com", response.getEmail());
      assertFalse(response.getIsVerified());

      verify(userRepository).save(any(User.class));
      verify(tokenRepository).save(any(Token.class));
      verify(emailService).sendVerificationEmail("test@example.com", "mockToken");
   }

   @Test
   void register_usernameAlreadyExists(){
      when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));

      assertNotNull(ex.getErrors());

      assertEquals("USERNAME_ALREADY_EXISTS", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("username"));
      assertTrue(ex.getErrors().get("username").contains("Username already exists"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_emailAlreadyExists(){
      when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));

      assertNotNull(ex.getErrors());

      assertEquals("EMAIL_ALREADY_REGISTERED", ex.getCode());
      assertEquals(HttpStatus.CONFLICT, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("email"));
      assertTrue(ex.getErrors().get("email").contains("Email already registered"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_passwordMismatch(){
      request.setConfirmPassword("wrongConfirmPassword");
      when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));

      assertNotNull(ex.getErrors());

      assertEquals("PASSWORDS_DO_NOT_MATCH", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      assertTrue(ex.getErrors().containsKey("confirmPassword"));
      assertTrue(ex.getErrors().get("confirmPassword").contains("Password confirmation does not match"));

      verify(userRepository, never()).save(any());
   }

   @Test
   void register_roleNotFound(){
      when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
      when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
      when(roleRepository.findByName(RoleName.CUSTOMER.name())).thenReturn(Optional.empty());

      ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));

      assertNotNull(ex.getErrors());

      assertEquals("DEFAULT_ROLE_NOT_FOUND", ex.getCode());
      assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

      assertTrue(ex.getErrors().containsKey("global"));
      assertTrue(ex.getErrors().get("global").contains("Registration Failed. Default role not found"));

      verify(userRepository, never()).save(any());
   }

}