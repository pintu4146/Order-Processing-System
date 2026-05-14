package com.orderprocessing.repository;

import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders with a given status.
     * Used by the list endpoint (status filter) and the scheduler (PENDING → PROCESSING).
     */
    List<Order> findByStatus(OrderStatus status);
}
