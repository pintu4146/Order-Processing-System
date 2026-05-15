package com.orderprocessing.repository;

import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders with a given status (unpaginated).
     * Used by the scheduler (PENDING → PROCESSING).
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find all orders with a given status (paginated).
     * Used by the list endpoint with status filter.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
