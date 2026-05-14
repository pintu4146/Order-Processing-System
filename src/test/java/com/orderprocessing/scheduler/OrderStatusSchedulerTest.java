package com.orderprocessing.scheduler;

import com.orderprocessing.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit test for OrderStatusScheduler.
 * Verifies the scheduler correctly delegates to the service layer.
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderStatusScheduler scheduler;

    @Test
    @DisplayName("Scheduler should call processPendingOrders on the service")
    void processPendingOrders_CallsService() {
        when(orderService.processPendingOrders()).thenReturn(3);

        scheduler.processPendingOrders();

        verify(orderService, times(1)).processPendingOrders();
    }

    @Test
    @DisplayName("Scheduler should handle exceptions gracefully without rethrowing")
    void processPendingOrders_ExceptionHandled_DoesNotRethrow() {
        when(orderService.processPendingOrders()).thenThrow(new RuntimeException("DB connection lost"));

        // Should NOT throw — the scheduler catches exceptions internally
        scheduler.processPendingOrders();

        verify(orderService, times(1)).processPendingOrders();
    }
}
