package com.monikoshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** لاگ رویدادهای امنیتی و اقدام‌های مدیر (ورود، تغییر رمز، تغییر قیمت/نقش و ...). فقط توسط ادمین دیده می‌شود. */
@Entity
@Table(name = "security_event",
        indexes = {
                @Index(name = "idx_security_event_created", columnList = "created_at"),
                @Index(name = "idx_security_event_type", columnList = "event_type"),
                @Index(name = "idx_security_event_ip", columnList = "ip"),
                @Index(name = "idx_security_event_email", columnList = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "details", length = 500)
    private String details;
}
