package com.orderprocessing.dto;

import java.math.BigDecimal;

public class OrderItemResponse {
    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal price;

    public OrderItemResponse() {}

    public OrderItemResponse(Long id, String productName, Integer quantity, BigDecimal price) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
