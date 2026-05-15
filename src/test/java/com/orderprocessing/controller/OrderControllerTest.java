package com.orderprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderprocessing.dto.CreateOrderRequest;
import com.orderprocessing.dto.OrderItemRequest;
import com.orderprocessing.dto.OrderItemResponse;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.exception.InvalidOrderStateException;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.model.OrderStatus;
import com.orderprocessing.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController using MockMvc.
 * Tests HTTP layer: request parsing, validation, status codes, response format.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderResponse buildSampleResponse() {
        return OrderResponse.builder()
                .id(1L)
                .customerName("John Doe")
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(List.of(OrderItemResponse.builder()
                        .id(1L)
                        .productName("Laptop")
                        .quantity(1)
                        .price(new BigDecimal("1200.00"))
                        .build()))
                .build();
    }

    // =========================================================================
    // POST /api/orders
    // =========================================================================
    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderEndpoint {

        @Test
        @DisplayName("Should return 201 for valid order request")
        void createOrder_ValidRequest_Returns201() throws Exception {
            OrderResponse response = buildSampleResponse();
            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("John Doe")
                    .items(List.of(OrderItemRequest.builder()
                            .productName("Laptop")
                            .quantity(1)
                            .price(new BigDecimal("1200.00"))
                            .build()))
                    .build();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 400 when items list is empty")
        void createOrder_EmptyItems_Returns400() throws Exception {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerName("John Doe")
                    .items(Collections.emptyList())
                    .build();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Should return 400 when customer name is missing")
        void createOrder_MissingCustomerName_Returns400() throws Exception {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .items(List.of(OrderItemRequest.builder()
                            .productName("Laptop")
                            .quantity(1)
                            .price(new BigDecimal("1200.00"))
                            .build()))
                    .build();

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // =========================================================================
    // GET /api/orders/{id}
    // =========================================================================
    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderByIdEndpoint {

        @Test
        @DisplayName("Should return 200 for existing order")
        void getOrderById_Exists_Returns200() throws Exception {
            OrderResponse response = buildSampleResponse();
            when(orderService.getOrderById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.customerName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void getOrderById_NotFound_Returns404() throws Exception {
            when(orderService.getOrderById(99L)).thenThrow(new OrderNotFoundException(99L));

            mockMvc.perform(get("/api/orders/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // =========================================================================
    // GET /api/orders
    // =========================================================================
    @Nested
    @DisplayName("GET /api/orders")
    class GetAllOrdersEndpoint {

        @Test
        @DisplayName("Should return 200 with paginated list of all orders")
        void getAllOrders_ReturnsAll() throws Exception {
            Page<OrderResponse> page =
                    new PageImpl<>(List.of(buildSampleResponse()));
            when(orderService.getAllOrders(eq(null), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 200 with filtered orders by status")
        void getAllOrders_FilteredByStatus_Returns200() throws Exception {
            Page<OrderResponse> page =
                    new PageImpl<>(List.of(buildSampleResponse()));
            when(orderService.getAllOrders(eq(OrderStatus.PENDING), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));
        }
    }

    // =========================================================================
    // PUT /api/orders/{id}/cancel
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/orders/{id}/cancel")
    class CancelOrderEndpoint {

        @Test
        @DisplayName("Should return 200 when cancelling PENDING order")
        void cancelOrder_PendingOrder_Returns200() throws Exception {
            OrderResponse response = buildSampleResponse();
            response.setStatus(OrderStatus.CANCELLED);
            when(orderService.cancelOrder(1L)).thenReturn(response);

            mockMvc.perform(put("/api/orders/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 409 when cancelling non-PENDING order")
        void cancelOrder_NonPendingOrder_Returns409() throws Exception {
            when(orderService.cancelOrder(1L)).thenThrow(
                    new InvalidOrderStateException("Cannot cancel order 1. Current status is PROCESSING."));

            mockMvc.perform(put("/api/orders/1/cancel"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message", containsString("PROCESSING")));
        }
    }
}
