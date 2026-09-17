package com.vietmart.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) đại diện thông tin sản phẩm nhận từ Product Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInfo {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String description;
    private String category;
    private String status;

    /**
     * Phương thức tiện ích trả về dữ liệu fallback an toàn khi gặp sự cố mạng hoặc timeout.
     */
    public static ProductInfo fallback(Long id) {
        return ProductInfo.builder()
                .id(id)
                .name("Sản phẩm tạm thời không khả dụng (Fallback)")
                .price(BigDecimal.ZERO)
                .stockQuantity(0)
                .description("Dữ liệu sản phẩm tạm thời gián đoạn do sự cố kết nối/timeout với product-service.")
                .category("UNKNOWN")
                .status("UNAVAILABLE")
                .build();
    }
}
