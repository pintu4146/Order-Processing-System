package com.orderprocessing.service;

import com.orderprocessing.dto.CreateOrderRequest;
import com.orderprocessing.dto.OrderItemRequest;
import com.orderprocessing.dto.OrderResponse;
import com.orderprocessing.exception.InvalidOrderStateException;
import com.orderprocessing.exception.OrderNotFoundException;
import com.orderprocessing.mapper.OrderMapper;
import com.orderprocessing.model.Order;
import com.orderprocessing.model.OrderItem;
import com.orderprocessing.model.OrderStatus;
import com.orderprocessing.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 * Uses Mockito to isolate the service layer from the database.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;
    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        // Build a reusable sample order
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productName("Laptop")
                .quantity(1)
                .price(new BigDecimal("1200.00"))
                .build();

        sampleOrder = Order.builder()
                .id(1L)
                .customerName("John Doe")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1200.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(sampleOrder);

        // Build a reusable valid request
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productName("Laptop")
                .quantity(1)
                .price(new BigDecimal("1200.00"))
                .build();
        validRequest = CreateOrderRequest.builder()
                .customerName("John Doe")
                .items(List.of(itemRequest))
                .build();
    }

    // =========================================================================
    // CREATE ORDER
    // =========================================================================
    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("Should create order with valid items and return PENDING status")
        void createOrder_WithValidItems_ReturnsPendingOrder() {
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                ReflectionTestUtils.setField(o, "id", 1L);
                return o;
            });

            OrderResponse response = orderService.createOrder(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getItems()).hasSize(1);
            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    // =========================================================================
    // GET ORDER BY ID
    // =========================================================================
    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Should return order when ID exists")
        void getOrderById_Exists_ReturnsOrder() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            OrderResponse response = orderService.getOrderById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCustomerName()).isEqualTo("John Doe");
            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when ID does not exist")
        void getOrderById_NotFound_ThrowsException() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(99L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // GET ALL ORDERS
    // =========================================================================
    @Nested
    @DisplayName("getAllOrders")
    class GetAllOrders {

        @Test
        @DisplayName("Should return all orders when no status filter (paginated)")
        void getAllOrders_NoFilter_ReturnsAll() {
            Page<Order> ordersPage = new PageImpl<>(List.of(sampleOrder));
            when(orderRepository.findAll(any(Pageable.class))).thenReturn(ordersPage);

            Page<OrderResponse> responses = orderService.getAllOrders(null, PageRequest.of(0, 10));

            assertThat(responses.getContent()).hasSize(1);
            verify(orderRepository).findAll(any(Pageable.class));
            verify(orderRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("Should return filtered orders when status provided (paginated)")
        void getAllOrders_WithStatusFilter_ReturnsFiltered() {
            Page<Order> ordersPage = new PageImpl<>(List.of(sampleOrder));
            when(orderRepository.findByStatus(eq(OrderStatus.PENDING), any(Pageable.class))).thenReturn(ordersPage);

            Page<OrderResponse> responses = orderService.getAllOrders(OrderStatus.PENDING, PageRequest.of(0, 10));

            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository).findByStatus(eq(OrderStatus.PENDING), any(Pageable.class));
            verify(orderRepository, never()).findAll(any(Pageable.class));
        }
    }

    // =========================================================================
    // CANCEL ORDER
    // =========================================================================
    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("Should cancel PENDING order successfully")
        void cancelOrder_PendingOrder_Success() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

            OrderResponse response = orderService.cancelOrder(1L);

            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(sampleOrder);
        }

        @Test
        @DisplayName("Should throw InvalidOrderStateException when cancelling PROCESSING order")
        void cancelOrder_ProcessingOrder_ThrowsException() {
            sampleOrder.setStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("PROCESSING");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidOrderStateException when cancelling SHIPPED order")
        void cancelOrder_ShippedOrder_ThrowsException() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("SHIPPED");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidOrderStateException when cancelling DELIVERED order")
        void cancelOrder_DeliveredOrder_ThrowsException() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("DELIVERED");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidOrderStateException when cancelling already CANCELLED order")
        void cancelOrder_AlreadyCancelled_ThrowsException() {
            sampleOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("CANCELLED");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when cancelling non-existent order")
        void cancelOrder_NotFound_ThrowsException() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(99L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // =========================================================================
    // PROCESS PENDING ORDERS (Scheduler)
    // =========================================================================
    @Nested
    @DisplayName("processPendingOrders")
    class ProcessPendingOrders {

        @Test
        @DisplayName("Should transition PENDING orders to PROCESSING")
        void processPendingOrders_WithPendingOrders_TransitionsAll() {
            List<Order> pendingOrders = List.of(sampleOrder);
            when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(pendingOrders);

            int count = orderService.processPendingOrders();

            assertThat(count).isEqualTo(1);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(orderRepository).saveAll(pendingOrders);
        }

        @Test
        @DisplayName("Should return 0 and perform no writes when no pending orders")
        void processPendingOrders_NoPendingOrders_NoOp() {
            when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(Collections.emptyList());

            int count = orderService.processPendingOrders();

            assertThat(count).isEqualTo(0);
            verify(orderRepository, never()).saveAll(any());
        }
    }
}
