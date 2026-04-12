package com.mraffi.ecommerce_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.constant.TokenStatus;
import com.mraffi.ecommerce_api.constant.TokenType;
import com.mraffi.ecommerce_api.dto.WebResponse;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.request.auth.ResendEmailVerificationRequest;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.entity.Token;
import com.mraffi.ecommerce_api.entity.User;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import com.mraffi.ecommerce_api.repository.UserRepository;
import com.mraffi.ecommerce_api.service.EmailService;
import com.mraffi.ecommerce_api.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTestIT {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private RoleRepository roleRepository;

   @Autowired
   private TokenRepository tokenRepository;

   @Autowired
   private JwtService jwtService;

   @MockBean
   private EmailService emailService;

   private User user;
   private Token token;

   String url = "/api/auth";

   @BeforeEach
   void setUp() {
      tokenRepository.deleteAllInBatch();
      userRepository.deleteAllInBatch();

      Role role = roleRepository.findByName(RoleName.CUSTOMER.name())
              .orElseGet(() -> roleRepository.save(Role.create(RoleName.CUSTOMER.name())));

      User userToSave = User.createLocalUser(
              "test_user_existing",
              "Test User",
              "existing@example.com",
              "hashedPassword",
              role
      );

      this.user = userRepository.saveAndFlush(userToSave);

      String realJwtToken = jwtService.generateEmailVerificationToken(this.user.getId());

      token = Token.create(
              realJwtToken,
              this.user,
              TokenType.EMAIL_ACTIVATION,
              Instant.now().plusMillis(900000L)
      );

      tokenRepository.saveAndFlush(token);
   }

   @Test
   void register_success() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .username("test_user")
              .fullname("Test User")
              .email("test@example.com")
              .password("123@Password456")
              .confirmPassword("123@Password456")
              .build();

      mockMvc.perform(
              post(url + "/register")
                      .accept(MediaType.APPLICATION_JSON)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<RegisterResponse> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<RegisterResponse>>() {}
                 );

                 assertNull(response.getErrors());
                 assertEquals("Registration Success. Please check your email for activation.", response.getMessage());

                 assertEquals(request.getUsername(), response.getData().getUsername());
                 assertEquals(request.getFullname(), response.getData().getFullname());
                 assertEquals(request.getEmail(), response.getData().getEmail());
                 assertFalse(response.getData().getIsVerified());

                 verify(emailService, times(1)).sendVerificationEmail(eq(request.getEmail()), anyString());
                 assertTrue(userRepository.existsById(response.getData().getId()));
              });
   }

   @Test
   void register_validationFailed() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .fullname("A".repeat(101))
              .email("invalid-email".repeat(101))
              .password("wrong")
              .build();

      mockMvc.perform(
              post(url + "/register")
                      .accept(MediaType.APPLICATION_JSON)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<RegisterResponse> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<RegisterResponse>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("VALIDATION_FAILED", response.getCode());

                 List<String> usernameErrors = response.getErrors().get("username");
                 assertNotNull(usernameErrors);
                 assertTrue(usernameErrors.contains("Username is required"));

                 List<String> fullnameErrors = response.getErrors().get("fullname");
                 assertNotNull(fullnameErrors);
                 assertTrue(fullnameErrors.contains("Fullname must not exceed 100 characters"));

                 List<String> emailErrors = response.getErrors().get("email");
                 assertNotNull(emailErrors);
                 assertTrue(emailErrors.containsAll(
                         List.of("Email is invalid", "Email must not exceed 100 characters"))
                 );

                 List<String> passwordErrors = response.getErrors().get("password");
                 assertNotNull(passwordErrors);
                 assertTrue(passwordErrors.containsAll(
                         List.of("Password must be at least 8 characters", "Password must contain at least 1 uppercase and 1 special character"))
                 );

                 List<String> confirmPassword = response.getErrors().get("confirmPassword");
                 assertNotNull(confirmPassword);
                 assertTrue(confirmPassword.contains("Confirm password is required"));
              });
   }

   @Test
   void register_usernameAlreadyExists() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .username(user.getUsername())
              .fullname("Test User")
              .email("different@example.com")
              .password("123@Password456")
              .confirmPassword("123@Password456")
              .build();

      mockMvc.perform(
                      post(url + "/register")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isConflict())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<RegisterResponse> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<RegisterResponse>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("USERNAME_ALREADY_EXISTS", response.getCode());

                 assertTrue(response.getErrors().containsKey("username"));
                 assertTrue(response.getErrors().get("username").contains("Username already exists"));

              });
   }

   @Test
   void register_emailAlreadyExists() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .username("different_user")
              .fullname("Test User")
              .email(user.getEmail())
              .password("123@Password456")
              .confirmPassword("123@Password456")
              .build();

      mockMvc.perform(
                      post(url + "/register")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isConflict())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<RegisterResponse> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<RegisterResponse>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("EMAIL_ALREADY_REGISTERED", response.getCode());

                 assertTrue(response.getErrors().containsKey("email"));
                 assertTrue(response.getErrors().get("email").contains("Email already registered"));

              });
   }

   @Test
   void register_passwordMismatch() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .username("different_username")
              .fullname("Test User")
              .email("different@example.com")
              .password("123@Password456")
              .confirmPassword("wrong")
              .build();

      mockMvc.perform(
                      post(url + "/register")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<RegisterResponse> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<RegisterResponse>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("PASSWORDS_DO_NOT_MATCH", response.getCode());

                 assertTrue(response.getErrors().containsKey("confirmPassword"));
                 assertTrue(response.getErrors().get("confirmPassword").contains("Password confirmation does not match"));

              });
   }

   @Test
   void verifyEmail_success() throws Exception {
      mockMvc.perform(
              get(url + "/verify-email")
                      .queryParam("token", token.getToken())
                      .accept(MediaType.APPLICATION_JSON)
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNull(response.getErrors());
                 assertEquals("Email verification successful", response.getMessage());
                 assertEquals("success", response.getData());

                 User updatedUser = userRepository.findById(user.getId()).orElseThrow();
                 Token updatedToken = tokenRepository.findByToken(token.getToken()).orElseThrow();

                 assertTrue(updatedUser.getIsVerified());
                 assertEquals(TokenStatus.USED, updatedToken.getTokenStatus());
              });
   }

   @Test
   void verifyEmail_failed_tokenNotFound() throws Exception {
      mockMvc.perform(
                      get(url + "/verify-email")
                              .queryParam("token", "token-not-found")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isNotFound())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("TOKEN_NOT_FOUND", response.getCode());

                 assertTrue(response.getErrors().containsKey("token"));
                 assertTrue(response.getErrors().get("token").contains("Invalid token"));
              });
   }

   @Test
   void verifyEmail_failed_tokenAlreadyUsed() throws Exception {
      token.setTokenStatus(TokenStatus.USED);
      tokenRepository.saveAndFlush(token);

      mockMvc.perform(
                      get(url + "/verify-email")
                              .queryParam("token", token.getToken())
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("TOKEN_ALREADY_USED", response.getCode());

                 assertTrue(response.getErrors().containsKey("token"));
                 assertTrue(response.getErrors().get("token").contains("This link has already been used. Please login."));
              });
   }

   @Test
   void verifyEmail_failed_expiredDb() throws Exception {
      setField(token, "expiredAt", Instant.now().minusSeconds(10));
      tokenRepository.saveAndFlush(token);

      mockMvc.perform(
                      get(url + "/verify-email")
                              .queryParam("token", token.getToken())
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("TOKEN_EXPIRED", response.getCode());

                 assertTrue(response.getErrors().containsKey("token"));
                 assertTrue(response.getErrors().get("token").contains("This link has expired. Please request a new verification link."));
                 assertTrue(response.getErrors().containsKey("email"));
                 assertTrue(response.getErrors().get("email").contains(user.getEmail()));

                 Token tokenDb = tokenRepository.findByToken(token.getToken()).orElseThrow();
                 assertEquals(TokenStatus.EXPIRED ,tokenDb.getTokenStatus());
              });
   }

   @Test
   void verifyEmail_failed_jwtMalformed() throws Exception {
      String malformedToken = token.getToken().replace(".", "");

      setField(token, "token", malformedToken);
      tokenRepository.saveAndFlush(token);

      mockMvc.perform(
                      get(url + "/verify-email")
                              .queryParam("token", malformedToken)
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("INVALID_TOKEN", response.getCode());
                 assertTrue(response.getErrors().get("token").contains("Invalid token format"));
              });
   }

   @Test
   void verifyEmail_failed_jwtInvalidSignature() throws Exception {
      String invalidSigToken = token.getToken().substring(0, token.getToken().length() - 1) + "Z";
      setField(token, "token", invalidSigToken);
      tokenRepository.saveAndFlush(token);

      mockMvc.perform(
                      get(url + "/verify-email")
                              .queryParam("token", invalidSigToken)
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("INVALID_SIGNATURE", response.getCode());
                 assertTrue(response.getErrors().get("token").contains("Invalid token signature"));
              });
   }

   @Test
   void resendEmailVerification_failed_userNotFound() throws Exception {
      ResendEmailVerificationRequest request = ResendEmailVerificationRequest.builder()
                      .email("notfound@mail.com").build();
      mockMvc.perform(
              post(url + "/resend-email-verification")
                      .accept(MediaType.APPLICATION_JSON)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request))
      ).andExpect(status().isNotFound())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<User> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<User>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("USER_NOT_FOUND", response.getCode());

                 assertTrue(response.getErrors().containsKey("global"));
                 assertTrue(response.getErrors().get("global").contains("User not found"));
              });
   }

   @Test
   void resendEmailVerification_failed_userHasVerified() throws Exception {
      user.verify();
      ResendEmailVerificationRequest request = ResendEmailVerificationRequest.builder()
                      .email(user.getEmail()).build();
      userRepository.saveAndFlush(user);

      mockMvc.perform(
                      post(url + "/resend-email-verification")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request))
              ).andExpect(status().isBadRequest())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNotNull(response.getErrors());
                 assertEquals("Request failed", response.getMessage());
                 assertEquals("USER_HAS_VERIFIED", response.getCode());

                 assertTrue(response.getErrors().containsKey("global"));
                 assertTrue(response.getErrors().get("global").contains("User has verified"));
              });
   }

   @Test
   void resendEmailVerification_success() throws Exception {
      tokenRepository.deleteAllInBatch();
      ResendEmailVerificationRequest request = ResendEmailVerificationRequest.builder()
              .email(user.getEmail()).build();

      mockMvc.perform(
                      post(url + "/resend-email-verification")
                              .accept(MediaType.APPLICATION_JSON)
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request))
              ).andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andDo(result -> {
                 WebResponse<String> response = objectMapper.readValue(
                         result.getResponse().getContentAsString(),
                         new TypeReference<WebResponse<String>>() {}
                 );

                 assertNull(response.getErrors());
                 assertEquals("Resend email verification success", response.getMessage());

                 verify(emailService, times(1)).sendVerificationEmail(eq(request.getEmail()), anyString());
              });
   }
}