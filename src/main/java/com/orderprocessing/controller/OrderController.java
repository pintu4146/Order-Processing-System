package com.orderprocessing.controller;

import com.orderprocessing.dto.CreateOrderRequest;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.model.OrderStatus;
import com.orderprocessing.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Endpoints for creating, retrieving, and cancelling orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the specified items. Initial status is PENDING.")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves detailed information about a specific order by its unique identifier.")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "ID of the order to retrieve") @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Retrieves a paginated list of all orders, optionally filtered by their current status.")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @Parameter(description = "Optional status to filter orders by") @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<OrderResponse> responses = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order if it is currently in PENDING status. Other statuses cannot be cancelled.")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "ID of the order to cancel") @PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }
}
