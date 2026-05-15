package com.orderprocessing.service;

import com.orderprocessing.dto.*;
import com.orderprocessing.exception.InvalidOrderStateException;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderItem;
import com.orderprocessing.model.OrderStatus;
import com.orderprocessing.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerName());

        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Map each item request to an OrderItem entity and link to the order
        request.getItems().forEach(itemRequest -> {
            OrderItem item = OrderItem.builder()
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .price(itemRequest.getPrice())
                    .build();
            order.addItem(item);
        });

        // Calculate total amount (price × quantity for each item)
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}, total: {}", savedOrder.getId(), total);
        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        Order order = findOrderOrThrow(id);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders;
        if (status != null) {
            log.debug("Fetching orders with status: {}", status);
            orders = orderRepository.findByStatus(status);
        } else {
            log.debug("Fetching all orders");
            orders = orderRepository.findAll();
        }
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null) {
            log.debug("Fetching orders with status: {} (page: {})", status, pageable.getPageNumber());
            ordersPage = orderRepository.findByStatus(status, pageable);
        } else {
            log.debug("Fetching all orders (page: {})", pageable.getPageNumber());
            ordersPage = orderRepository.findAll(pageable);
        }
        return ordersPage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Attempting to cancel order with id: {}", id);
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order " + id + ". Current status is " + order.getStatus()
                            + ". Only PENDING orders can be cancelled."
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", id);
        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional
    public int processPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        if (pendingOrders.isEmpty()) {
            log.debug("No pending orders to process");
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        pendingOrders.forEach(order -> {
            order.setStatus(OrderStatus.PROCESSING);
            order.setUpdatedAt(now);
        });

        orderRepository.saveAll(pendingOrders);
        log.info("Transitioned {} orders from PENDING to PROCESSING", pendingOrders.size());
        return pendingOrders.size();
    }

    // ========== Private Helpers ==========

    /**
     * Fetch an order by ID or throw OrderNotFoundException.
     * Centralizes the "find or 404" pattern — DRY.
     */
    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Map an Order entity to an OrderResponse DTO.
     * Entities are NEVER returned from public methods — always mapped to DTOs.
     */
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(this::mapToItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Map an OrderItem entity to an OrderItemResponse DTO.
     */
    private OrderItemResponse mapToItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
