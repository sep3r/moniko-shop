package com.monikoshop.controller;

import com.monikoshop.dto.LogDtos.PageDto;
import com.monikoshop.dto.LogDtos.SecurityEventDto;
import com.monikoshop.dto.LogDtos.VisitLogDto;
import com.monikoshop.entity.SecurityEvent;
import com.monikoshop.entity.User;
import com.monikoshop.entity.VisitLog;
import com.monikoshop.repository.SecurityEventRepository;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.repository.VisitLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** لاگ IP بازدیدکنندگان (مهمان/کاربر) و رویدادهای امنیتی؛ فقط ادمین (مسیر /api/admin/** در SecurityConfig). */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private static final int MAX_PAGE_SIZE = 100;

    private final VisitLogRepository visitLogRepository;
    private final SecurityEventRepository securityEventRepository;
    private final UserRepository userRepository;

    /**
     * @param kind  all | guest | user
     * @param ip    ابتدای آدرس IP (مثلاً 5.200)
     * @param date  yyyy-MM-dd
     */
    @GetMapping("/visits")
    public ResponseEntity<PageDto<VisitLogDto>> visits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "all") String kind,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String date) {

        Specification<VisitLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if ("guest".equals(kind)) {
                p.add(cb.isNull(root.get("userId")));
            } else if ("user".equals(kind)) {
                p.add(cb.isNotNull(root.get("userId")));
            }
            if (ip != null && !ip.isBlank()) {
                p.add(cb.like(root.get("ip"), ip.trim() + "%"));
            }
            LocalDate d = parseDate(date);
            if (d != null) {
                p.add(cb.equal(root.get("visitDate"), d));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        Page<VisitLog> result = visitLogRepository.findAll(spec, pageRequest(page, size, "lastSeen"));

        Set<Long> userIds = result.getContent().stream()
                .map(VisitLog::getUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> emails = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userRepository.findAllById(userIds)) {
                emails.put(u.getId(), u.contactLabel());
            }
        }

        List<VisitLogDto> items = result.getContent().stream()
                .map(v -> new VisitLogDto(v.getId(), v.getVisitDate(), v.getVisitorId(), v.getUserId(),
                        v.getUserId() == null ? null : emails.get(v.getUserId()),
                        v.getIp(), v.getUserAgent(), v.getFirstSeen(), v.getLastSeen()))
                .toList();
        return ResponseEntity.ok(toPage(result, items));
    }

    /**
     * @param type نوع رویداد (مثلاً LOGIN_FAILURE)؛ خالی = همه
     * @param q    بخشی از ایمیل یا ابتدای IP
     */
    @GetMapping("/security")
    public ResponseEntity<PageDto<SecurityEventDto>> security(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q) {

        Specification<SecurityEvent> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (type != null && !type.isBlank()) {
                p.add(cb.equal(root.get("eventType"), type.trim()));
            }
            if (q != null && !q.isBlank()) {
                String term = q.trim().toLowerCase();
                p.add(cb.or(
                        cb.like(cb.lower(root.get("email")), "%" + term + "%"),
                        cb.like(root.get("ip"), term + "%")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        Page<SecurityEvent> result = securityEventRepository.findAll(spec, pageRequest(page, size, "createdAt"));
        List<SecurityEventDto> items = result.getContent().stream()
                .map(e -> new SecurityEventDto(e.getId(), e.getCreatedAt(), e.getEventType(), e.getUserId(),
                        e.getEmail(), e.getIp(), e.getUserAgent(), e.getDetails()))
                .toList();
        return ResponseEntity.ok(toPage(result, items));
    }

    private static PageRequest pageRequest(int page, int size, String sortField) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortField, "id"));
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static <T> PageDto<T> toPage(Page<?> page, List<T> items) {
        return new PageDto<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
