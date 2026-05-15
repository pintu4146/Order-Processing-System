package com.orderprocessing.mapper;

import com.orderprocessing.dto.OrderItemResponse;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderItem;
import org.springframework.stereotype.Component;

/**
 * Maps between Order entities and DTOs.
 * Extracted from OrderServiceImpl for Single Responsibility.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(this::toItemResponse)
                        .toList())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
