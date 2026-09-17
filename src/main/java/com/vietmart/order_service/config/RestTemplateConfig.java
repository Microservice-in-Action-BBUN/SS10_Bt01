package com.vietmart.order_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cấu hình RestTemplate tập trung cho Order Service:
 * - @LoadBalanced: Kích hoạt bộ chặn Client-side Load Balancing (Spring Cloud LoadBalancer),
 *   cho phép phân giải Logical Service ID (http://product-service) qua Eureka Service Discovery.
 * - Cấu hình Timeout: connectTimeout = 2 giây, readTimeout = 3 giây nhằm ngăn chặn nghẽn socket
 *   và giải phóng Tomcat Worker Threads khi service hạ tầng bị chậm trễ.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Cấu hình connectTimeout = 2s
        factory.setConnectTimeout(Duration.ofSeconds(2));
        // Cấu hình readTimeout = 3s
        factory.setReadTimeout(Duration.ofSeconds(3));
        return new RestTemplate(factory);
    }
}
