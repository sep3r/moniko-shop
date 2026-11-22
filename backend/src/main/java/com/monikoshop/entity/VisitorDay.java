package com.monikoshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * یک ردیف برای هر «بازدیدکننده یکتا» در هر روز.
 * userId پر باشد یعنی آن روز با حساب کاربری وارد شده؛ خالی یعنی مهمان.
 * این جدول فقط شناسه‌ی تصادفی مرورگر را دارد؛ IP و User-Agent در جدول visit_log ثبت می‌شوند.
 */
@Entity
@Table(name = "visitor_day",
        uniqueConstraints = @UniqueConstraint(name = "uk_visitor_day", columnNames = {"visit_date", "visitor_id"}),
        indexes = @Index(name = "idx_visitor_day_date", columnList = "visit_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visitor_id", nullable = false, length = 36)
    private String visitorId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;
}
