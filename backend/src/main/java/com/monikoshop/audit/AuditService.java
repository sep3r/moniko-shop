package com.monikoshop.audit;

import com.monikoshop.entity.SecurityEvent;
import com.monikoshop.entity.User;
import com.monikoshop.repository.SecurityEventRepository;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.security.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * ثبت رویدادهای امنیتی در دیتابیس (با IP و User-Agent درخواست جاری).
 * هر رویداد در تراکنش جداگانه ذخیره می‌شود تا حتی وقتی درخواست اصلی خطا می‌دهد
 * (مثلاً ورود ناموفق) ثبت بماند؛ و خطای لاگ هرگز درخواست اصلی را خراب نمی‌کند.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final SecurityEventRepository repository;
    private final UserRepository userRepository;
    private final TransactionTemplate requiresNew;

    @Value("${AUDIT_RETENTION_DAYS:365}")
    private int retentionDays;

    public AuditService(SecurityEventRepository repository,
                        UserRepository userRepository,
                        PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** رویداد با کاربر واردشده‌ی فعلی (برای اقدام‌های مدیر و کاربر). */
    public void log(String type, String details) {
        String email = null;
        Long userId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            email = auth.getName();
            try {
                userId = userRepository.findByEmail(email).map(User::getId).orElse(null);
            } catch (Exception ignored) {
                // لاگ نباید مانع کار اصلی شود
            }
        }
        log(type, userId, email, details);
    }

    /** رویداد با ایمیل/کاربر مشخص (مثلاً ایمیلی که در ورود ناموفق وارد شده). */
    public void log(String type, Long userId, String email, String details) {
        try {
            HttpServletRequest request = currentRequest();
            SecurityEvent event = SecurityEvent.builder()
                    .createdAt(Instant.now())
                    .eventType(type)
                    .userId(userId)
                    .email(clip(email, 100))
                    .ip(request == null ? null : clip(ClientIp.of(request), 64))
                    .userAgent(request == null ? null : ClientIp.userAgent(request))
                    .details(clip(details, 500))
                    .build();
            requiresNew.executeWithoutResult(status -> repository.save(event));
        } catch (Exception e) {
            log.warn("audit log failed ({}): {}", type, e.getMessage());
        }
    }

    /** هر شب رویدادهای قدیمی‌تر از مدت نگهداری پاک می‌شوند. */
    @Scheduled(cron = "0 20 0 * * *", zone = "${tracking.zone:Asia/Tehran}")
    public void pruneOld() {
        try {
            int deleted = repository.deleteOlderThan(Instant.now().minus(Duration.ofDays(Math.max(30, retentionDays))));
            if (deleted > 0) {
                log.info("audit: pruned {} old security events", deleted);
            }
        } catch (Exception e) {
            log.warn("audit prune failed: {}", e.getMessage());
        }
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        String v = s.replaceAll("[\\r\\n]", " ");
        return v.length() > max ? v.substring(0, max) : v;
    }
}
