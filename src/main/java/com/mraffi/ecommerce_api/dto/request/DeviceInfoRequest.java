package com.mraffi.ecommerce_api.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DeviceInfoRequest {

   private String ip;
   private String userAgent;

}
