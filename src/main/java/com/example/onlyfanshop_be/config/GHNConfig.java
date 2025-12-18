package com.example.onlyfanshop_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GHNConfig {
    
    @Value("${GHN_API_URL:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String apiUrl;
    
    @Value("${GHN_TOKEN:}")
    private String token;
    
    @Value("${GHN_SHOP_ID:0}")
    private Integer shopId;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getToken() {
        return token;
    }
    
    public Integer getShopId() {
        return shopId;
    }
}
