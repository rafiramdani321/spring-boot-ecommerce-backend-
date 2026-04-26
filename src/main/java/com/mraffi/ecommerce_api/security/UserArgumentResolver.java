package com.mraffi.ecommerce_api.security;

import com.mraffi.ecommerce_api.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {
   @Override
   public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(CurrentUser.class);
   }

   @Override
   public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
      HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
      Object userId = request.getAttribute("userId");

      if(userId == null){
         throw new ApiException(
                 "UNAUTHORIZED",
                 HttpStatus.UNAUTHORIZED,
                 Map.of("global", List.of("Unauthorized"))
         );
      }

      return userId;
    }
}
