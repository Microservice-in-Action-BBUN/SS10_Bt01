package com.vietmart.order_service.exception;

/**
 * Ngoại lệ nghiệp vụ khi không tìm thấy thông tin sản phẩm trong product-service (HTTP 404).
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }
}
