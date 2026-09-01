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
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_TOPIC = "user-events";

    public void sendUserRegisteredEvent(Long userId, String email, String fullName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_REGISTERED");
        event.put("userId", userId);
        event.put("email", email);
        event.put("fullName", fullName);
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(USER_TOPIC, email, event);
            log.info("User registered event sent for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user registered event: {}", e.getMessage());
        }
    }

    public void sendUserLoginEvent(Long userId, String email) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_LOGIN");
        event.put("userId", userId);
        event.put("email", email);
        event.put("timestamp", System.currentTimeMillis());

        try {
            kafkaTemplate.send(USER_TOPIC, email, event);
            log.info("User login event sent for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user login event: {}", e.getMessage());
        }
    }
}
