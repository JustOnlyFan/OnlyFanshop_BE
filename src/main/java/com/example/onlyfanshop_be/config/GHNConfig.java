package com.example.onlyfanshop_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * GHN Configuration - Cấu hình cho GHN API Integration
 */
@Configuration
public class GHNConfig {
    
    @Value("${GHN_API_URL:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String apiUrl;
    
    @Value("${GHN_TOKEN:}")
    private String token;
    
    @Value("${GHN_SHOP_ID:0}")
    private Integer shopId;
    
    @Value("${GHN_CONNECT_TIMEOUT:5000}")
    private int connectTimeout;
    
    @Value("${GHN_READ_TIMEOUT:30000}")
    private int readTimeout;
    
    /**
     * RestTemplate bean với cấu hình timeout
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .build();
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
    
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
}
