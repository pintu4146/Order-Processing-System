package com.orderprocessing.service;

import com.orderprocessing.dto.CreateOrderRequest;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.exception.InvalidOrderStateException;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.mapper.OrderMapper;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

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
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        Order order = findOrderOrThrow(id);
        return orderMapper.toResponse(order);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null) {
            log.debug("Fetching orders with status: {} (page: {})", status, pageable.isPaged() ? pageable.getPageNumber() : "unpaged");
            ordersPage = orderRepository.findByStatus(status, pageable);
        } else {
            log.debug("Fetching all orders (page: {})", pageable.isPaged() ? pageable.getPageNumber() : "unpaged");
            ordersPage = orderRepository.findAll(pageable);
        }
        return ordersPage.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Attempting to cancel order with id: {}", id);
        Order order = findOrderOrThrow(id);

        // Delegate transition validation to the enum — OCP
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order " + id + ". Current status is " + order.getStatus()
                            + ". Only PENDING orders can be cancelled."
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        // @UpdateTimestamp handles updatedAt automatically — no manual set needed
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", id);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public int processPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        if (pendingOrders.isEmpty()) {
            log.debug("No pending orders to process");
            return 0;
        }

        pendingOrders.forEach(order -> {
            order.getStatus().validateTransition(OrderStatus.PROCESSING);
            order.setStatus(OrderStatus.PROCESSING);
            // @UpdateTimestamp handles updatedAt automatically
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
}
