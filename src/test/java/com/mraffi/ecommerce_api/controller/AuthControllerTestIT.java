package com.mraffi.ecommerce_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mraffi.ecommerce_api.constant.RoleName;
import com.mraffi.ecommerce_api.dto.WebResponse;
import com.mraffi.ecommerce_api.dto.request.auth.RegisterRequest;
import com.mraffi.ecommerce_api.dto.response.auth.RegisterResponse;
import com.mraffi.ecommerce_api.entity.Role;
import com.mraffi.ecommerce_api.repository.RoleRepository;
import com.mraffi.ecommerce_api.repository.TokenRepository;
import com.mraffi.ecommerce_api.repository.UserRepository;
import com.mraffi.ecommerce_api.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

   @MockBean
   private EmailService emailService;

   String url = "/api/auth";

   @BeforeEach
   void setUp(){
      tokenRepository.deleteAll();
      userRepository.deleteAll();

      if(roleRepository.findByName(RoleName.CUSTOMER.name()).isEmpty()){
         Role role = Role.create(RoleName.CUSTOMER.name());
         roleRepository.save(role);
      }
   }

   @Test
   void register_success() throws Exception {
      RegisterRequest request = RegisterRequest.builder()
              .username("testuser")
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
                 assertEquals("Registration Success. Please check your email for activation", response.getMessage());

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
              .email("emailerror".repeat(101))
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

      register_success();

      RegisterRequest request = RegisterRequest.builder()
              .username("testuser")
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

      register_success();

      RegisterRequest request = RegisterRequest.builder()
              .username("differentuser")
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
              .username("testuser")
              .fullname("Test User")
              .email("example@example.com")
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

}