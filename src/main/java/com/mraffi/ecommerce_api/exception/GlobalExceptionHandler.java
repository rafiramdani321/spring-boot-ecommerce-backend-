package com.mraffi.ecommerce_api.exception;

import com.mraffi.ecommerce_api.dto.WebResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(ApiException.class)
   public ResponseEntity<WebResponse<Object>> handleApiException(ApiException ex){
      WebResponse<Object> response = WebResponse.<Object>builder()
              .message("Request failed")
              .code(ex.getCode())
              .errors(ex.getErrors())
              .build();
      return ResponseEntity
              .status(ex.getStatus())
              .body(response);
   }

   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<WebResponse<Object>> handleValidationException(MethodArgumentNotValidException ex){
      Map<String, List<String>> errors = ex.getBindingResult()
              .getFieldErrors()
              .stream()
              .collect(Collectors.groupingBy(
                      FieldError::getField,
                      Collectors.mapping(
                              DefaultMessageSourceResolvable::getDefaultMessage,
                              Collectors.toList()
                      )
              ));

      WebResponse<Object> response = WebResponse.<Object>builder()
              .message("Request failed")
              .code("VALIDATION_FAILED")
              .errors(errors)
              .build();

      return ResponseEntity.badRequest().body(response);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<WebResponse<Object>> handleGeneralException(Exception ex){
      WebResponse<Object> response = WebResponse.<Object>builder()
              .message("Internal server error")
              .code("INTERNAL_SERVER_ERROR")
              .build();

      return ResponseEntity.internalServerError().body(response);
   }

}
