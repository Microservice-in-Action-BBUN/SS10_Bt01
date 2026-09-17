package com.vietmart.order_service.client;

import com.vietmart.order_service.dto.ProductInfo;
import com.vietmart.order_service.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceClientRTTest {

    @Mock
    private RestTemplate restTemplate;

    private ProductServiceClientRT productServiceClientRT;

    private static final String SERVICE_URL = "http://product-service/api/products/{id}";

    @BeforeEach
    void setUp() {
        // Khởi tạo ProductServiceClientRT với Mock RestTemplate và base URL logic
        productServiceClientRT = new ProductServiceClientRT(restTemplate, "http://product-service");
    }

    @Test
    @DisplayName("Unit Test 1: Happy Path - Gọi thành công và nhận thông tin sản phẩm chính xác")
    void testGetById_Success() {
        // 1. Arrange: Chuẩn bị dữ liệu mẫu và hành vi mock
        Long productId = 101L;
        ProductInfo expectedProduct = ProductInfo.builder()
                .id(productId)
                .name("Sữa tươi tiệt trùng VietMart Organic")
                .price(new BigDecimal("35000"))
                .stockQuantity(150)
                .description("Sữa tươi sạch từ trang trại VietMart")
                .category("Thực phẩm")
                .status("AVAILABLE")
                .build();

        when(restTemplate.getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId)))
                .thenReturn(expectedProduct);

        // 2. Act: Thực thi phương thức gọi
        ProductInfo actualProduct = productServiceClientRT.getById(productId);

        // 3. Assert: Kiểm tra kết quả trả về
        assertNotNull(actualProduct, "Kết quả trả về không được null khi gọi thành công");
        assertEquals(productId, actualProduct.getId());
        assertEquals("Sữa tươi tiệt trùng VietMart Organic", actualProduct.getName());
        assertEquals(new BigDecimal("35000"), actualProduct.getPrice());
        assertEquals(150, actualProduct.getStockQuantity());
        assertEquals("AVAILABLE", actualProduct.getStatus());

        // Verify: Xác nhận RestTemplate được gọi đúng 1 lần với URL logic và params chuẩn
        verify(restTemplate, times(1)).getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId));
    }

    @Test
    @DisplayName("Unit Test 2: Timeout Simulation - Bắt ResourceAccessException và kích hoạt Fallback")
    void testGetById_Timeout_ReturnsFallback() {
        // 1. Arrange: Mô phỏng RestTemplate ném ResourceAccessException (mô phỏng read timeout sau 3s)
        Long productId = 999L;
        when(restTemplate.getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId)))
                .thenThrow(new ResourceAccessException("I/O error on GET request: Read timed out after 3000ms"));

        // 2. Act: Gọi phương thức
        ProductInfo result = productServiceClientRT.getById(productId);

        // 3. Assert: Kiểm tra đối tượng fallback được trả về an toàn, không làm gián đoạn luồng
        assertNotNull(result, "Fallback object không được null");
        assertEquals(productId, result.getId());
        assertEquals("Sản phẩm tạm thời không khả dụng (Fallback)", result.getName());
        assertEquals(BigDecimal.ZERO, result.getPrice());
        assertEquals(0, result.getStockQuantity());
        assertEquals("UNAVAILABLE", result.getStatus());

        // Verify: Xác nhận RestTemplate đã được gọi đúng 1 lần
        verify(restTemplate, times(1)).getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId));
    }

    @Test
    @DisplayName("Unit Test 3: 404 Not Found - Bắt HttpClientErrorException.NotFound và ném ProductNotFoundException")
    void testGetById_NotFound_ThrowsProductNotFoundException() {
        // 1. Arrange: Mô phỏng phản hồi HTTP 404 Not Found từ product-service
        Long productId = 404L;
        when(restTemplate.getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // 2. Act & Assert: Xác minh phương thức ném đúng ngoại lệ nghiệp vụ ProductNotFoundException
        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () -> {
            productServiceClientRT.getById(productId);
        }, "Khi sản phẩm không tồn tại (404), phải ném ProductNotFoundException");

        assertTrue(exception.getMessage().contains("404"), "Message lỗi phải chứa ID sản phẩm");
        verify(restTemplate, times(1)).getForObject(eq(SERVICE_URL), eq(ProductInfo.class), eq(productId));
    }
}
