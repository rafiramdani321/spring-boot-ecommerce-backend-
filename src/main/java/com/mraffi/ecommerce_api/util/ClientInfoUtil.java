package com.mraffi.ecommerce_api.util;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class ClientInfoUtil {

   public static Map<String, String> getClientInfo(HttpServletRequest request){
      String xForwardFor = request.getHeader("X-Forwarded-for");
      String ip;

      if(xForwardFor != null && !xForwardFor.isEmpty()){
         ip = xForwardFor.split(",")[0];
      } else {
         ip = request.getRemoteAddr();
      }

      String userAgent = request.getHeader("User-Agent");
      if(userAgent == null){
         userAgent = "unknown";
      }

      return Map.of(
              "ip", ip,
              "userAgent", userAgent
      );
   }

   public static String generateDeviceHash(String ip, String userAgent){
      try{
         String raw = ip + ":" + userAgent;
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

         return HexFormat.of().formatHex(hashBytes);
      }catch (NoSuchAlgorithmException e){
         throw new RuntimeException("Error generate device hash", e);
      }
   }

}
