package com.orderprocessing.service;

import com.orderprocessing.dto.CreateOrderRequest;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.model.OrderStatus;

import java.util.List;

/**
 * Service interface for order operations.
 * All business logic is encapsulated behind this contract.
 */
public interface OrderService {

    /**
     * Create a new order from the given request. The order starts in PENDING status.
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Retrieve an order by its ID.
     * @throws com.orderprocessing.exception.OrderNotFoundException if not found
     */
    OrderResponse getOrderById(Long id);

    /**
     * List all orders, optionally filtered by status.
     * @param status if null, returns all orders; otherwise filters by the given status
     */
    List<OrderResponse> getAllOrders(OrderStatus status);

    /**
     * Cancel an order. Only PENDING orders can be cancelled.
     * @throws com.orderprocessing.exception.OrderNotFoundException if not found
     * @throws com.orderprocessing.exception.InvalidOrderStateException if not PENDING
     */
    OrderResponse cancelOrder(Long id);

    /**
     * Transition all PENDING orders to PROCESSING.
     * Called by the scheduler. Idempotent — returns 0 if no PENDING orders exist.
     * @return the number of orders transitioned
     */
    int processPendingOrders();
}
