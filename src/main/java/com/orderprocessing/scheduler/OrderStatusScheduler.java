package com.orderprocessing.scheduler;

import com.orderprocessing.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderService orderService;

    /**
     * Automatically moves PENDING orders to PROCESSING.
     * Interval is configured in application.yml.
     */
    @Scheduled(fixedRateString = "${app.scheduler.interval}", initialDelay = 10000)
    public void processPendingOrders() {
        log.info("Scheduled task started: Transitioning PENDING orders to PROCESSING...");
        try {
            int count = orderService.processPendingOrders();
            if (count > 0) {
                log.info("Scheduled task completed: {} orders transitioned.", count);
            } else {
                log.debug("Scheduled task completed: No pending orders found.");
            }
        } catch (Exception e) {
            log.error("Scheduled task failed: Error during order status transition", e);
        }
    }
}
