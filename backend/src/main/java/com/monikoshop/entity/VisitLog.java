package com.monikoshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * لاگ بازدید: یک ردیف برای هر ترکیب «روز + مرورگر + IP».
 * userId پر باشد یعنی با حساب کاربری وارد بوده؛ خالی یعنی مهمان.
 * IP و User-Agent داده‌ی شخصی حساب می‌شوند: بعد از {@code VISIT_LOG_RETENTION_DAYS} (پیش‌فرض ۹۰ روز) پاک می‌شوند.
 */
@Entity
@Table(name = "visit_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_visit_log", columnNames = {"visit_date", "visitor_id", "ip"}),
        indexes = {
                @Index(name = "idx_visit_log_date", columnList = "visit_date"),
                @Index(name = "idx_visit_log_ip", columnList = "ip"),
                @Index(name = "idx_visit_log_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visitor_id", nullable = false, length = 36)
    private String visitorId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ip", nullable = false, length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;
}
