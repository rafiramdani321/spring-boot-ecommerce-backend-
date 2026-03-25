package com.mraffi.ecommerce_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

   private final String code;
   private final HttpStatus status;
   private final Map<String, List<String>> errors;

   public ApiException(String code, HttpStatus status, Map<String, List<String>> errors){
      super(code);
      this.code = code;
      this.status = status;
      this.errors = errors;
   }

}
