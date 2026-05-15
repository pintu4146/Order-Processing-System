package com.orderprocessing.model;

import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle states with built-in transition rules.
 * Adding a new status only requires updating VALID_TRANSITIONS — no service changes.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            PENDING,    Set.of(PROCESSING, CANCELLED),
            PROCESSING, Set.of(SHIPPED),
            SHIPPED,    Set.of(DELIVERED),
            DELIVERED,  Set.of(),
            CANCELLED,  Set.of()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void validateTransition(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition from " + this + " to " + target
                            + ". Allowed transitions: " + VALID_TRANSITIONS.getOrDefault(this, Set.of()));
        }
    }
}
