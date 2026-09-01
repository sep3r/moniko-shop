package com.monikoshop.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-events";

    public void sendOrderCreatedEvent(Long orderId, Long userId, java.math.BigDecimal totalAmount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ORDER_CREATED");
        event.put("orderId", orderId);
        event.put("userId", userId);
        event.put("totalAmount", totalAmount);
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(ORDER_TOPIC, String.valueOf(orderId), event);
        } catch (Exception e) {
            // Kafka being unavailable must never block a purchase, so this is logged
            // and swallowed rather than propagated to the caller.
            log.error("Failed to send order created event: {}", e.getMessage());
        }
    }

    public void sendOrderStatusChangedEvent(Long orderId, String newStatus) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "ORDER_STATUS_CHANGED");
        event.put("orderId", orderId);
        event.put("status", newStatus);
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(ORDER_TOPIC, String.valueOf(orderId), event);
        } catch (Exception e) {
            log.error("Failed to send order status changed event: {}", e.getMessage());
        }
    }
}
