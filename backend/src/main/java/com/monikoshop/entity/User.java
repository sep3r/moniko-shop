package com.monikoshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** اختیاری: کاربران جدید با شماره موبایل ثبت‌نام می‌کنند (ایمیل فقط برای حساب‌های قدیمی/مدیر). */
    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** شماره‌ی نرمال‌شده 09xxxxxxxxx؛ یکتا. */
    @Column(length = 20)
    private String phone;

    /** true یعنی مالک شماره با کد پیامکی آن را تأیید کرده است. */
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** آخرین زمان ورود موفق کاربر (UTC). null یعنی هنوز وارد نشده. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** برچسب نمایشی برای لاگ/پنل: شماره، وگرنه ایمیل. */
    public String contactLabel() {
        return phone != null && !phone.isBlank() ? phone : email;
    }
}
