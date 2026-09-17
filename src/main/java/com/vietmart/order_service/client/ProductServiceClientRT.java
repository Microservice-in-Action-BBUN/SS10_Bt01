package com.vietmart.order_service.client;

import com.vietmart.order_service.dto.ProductInfo;
import com.vietmart.order_service.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ProductServiceClientRT - Đã khắc phục triệt để 3 lỗi kỹ thuật:
 * 1. Tiêm Bean RestTemplate được gắn @LoadBalanced từ Spring IoC Container.
 * 2. Sử dụng Logical Service ID "product-service" thay vì hardcode IP vật lý:
 *    Địa chỉ logic: http://product-service/api/products/{id}
 * 3. RestTemplate đã được cấu hình connectTimeout (2s) và readTimeout (3s).
 * 4. Xử lý phân biệt rõ ràng:
 *    - ResourceAccessException (Timeout / Lỗi I/O mạng) -> Trả về fallback an toàn.
 *    - HttpClientErrorException.NotFound (Mã 404) -> Ném ProductNotFoundException nghiệp vụ.
 */
@Component
@Slf4j
public class ProductServiceClientRT {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    @Autowired
    public ProductServiceClientRT(
            @LoadBalanced RestTemplate restTemplate,
            @Value("${services.product-service.url:http://product-service}") String productServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    // Constructor phục vụ Unit Testing với Mockito
    public ProductServiceClientRT(RestTemplate restTemplate) {
        this(restTemplate, "http://product-service");
    }

    /**
     * Lấy thông tin chi tiết một sản phẩm theo ID.
     *
     * @param productId Mã sản phẩm cần truy vấn
     * @return Thông tin sản phẩm hoặc đối tượng Fallback nếu xảy ra Timeout
     * @throws ProductNotFoundException Nếu product-service trả về mã HTTP 404
     */
    public ProductInfo getById(Long productId) {
        String url = productServiceUrl + "/api/products/{id}";
        log.info("Đang gửi yêu cầu GET tới product-service qua RestTemplate: {}", url);

        try {
            return restTemplate.getForObject(url, ProductInfo.class, productId);
        } catch (ResourceAccessException e) {
            // LỖI TIMEOUT / KẾT NỐI MẠNG: Kích hoạt fallback để giải phóng thread và bảo vệ Order Service
            log.error("ResourceAccessException (Timeout/Connection error) với sản phẩm ID {}: {}. Kích hoạt Fallback.",
                    productId, e.getMessage());
            return ProductInfo.fallback(productId);
        } catch (HttpClientErrorException.NotFound e) {
            // LỖI 404 NOT FOUND: Sản phẩm không tồn tại -> Ném ngoại lệ nghiệp vụ cho tầng xử lý phía trên
            log.warn("Sản phẩm với ID {} không tồn tại trên product-service (404 Not Found).", productId);
            throw new ProductNotFoundException(productId);
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm.
     *
     * @return Danh sách ProductInfo hoặc danh sách rỗng nếu xảy ra Timeout
     */
    public List<ProductInfo> getAll() {
        String url = productServiceUrl + "/api/products";
        log.info("Đang gửi yêu cầu GET lấy danh sách sản phẩm: {}", url);

        try {
            ProductInfo[] products = restTemplate.getForObject(url, ProductInfo[].class);
            return products != null ? Arrays.asList(products) : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("ResourceAccessException khi lấy danh sách sản phẩm. Kích hoạt fallback danh sách rỗng.");
            return Collections.emptyList();
        }
    }
}
