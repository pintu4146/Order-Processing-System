package com.orderprocessing.service;

import com.orderprocessing.dto.*;
import com.orderprocessing.exception.InvalidOrderStateException;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderItem;
import com.orderprocessing.model.OrderStatus;
import com.orderprocessing.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerName());

        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setStatus(OrderStatus.PENDING);

        // Map each item request to an OrderItem entity and link to the order
        request.getItems().forEach(itemRequest -> {
            OrderItem item = new OrderItem();
            item.setProductName(itemRequest.getProductName());
            item.setQuantity(itemRequest.getQuantity());
            item.setPrice(itemRequest.getPrice());
            order.addItem(item);
        });

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());
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
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerName(order.getCustomerName());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(order.getItems().stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList()));
        return response;
    }

    /**
     * Map an OrderItem entity to an OrderItemResponse DTO.
     */
    private OrderItemResponse mapToItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductName(item.getProductName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        return response;
    }
}
