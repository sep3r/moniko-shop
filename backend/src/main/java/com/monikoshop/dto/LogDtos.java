package com.monikoshop.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** پاسخ‌های لاگ بازدید و رویدادهای امنیتی برای پنل مدیریت. */
public final class LogDtos {

    private LogDtos() { }

    public record VisitLogDto(Long id, LocalDate date, String visitorId, Long userId, String userEmail,
                              String ip, String userAgent, Instant firstSeen, Instant lastSeen) { }

    public record SecurityEventDto(Long id, Instant createdAt, String type, Long userId, String email,
                                   String ip, String userAgent, String details) { }

    public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) { }
}
