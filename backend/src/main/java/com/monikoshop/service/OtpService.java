package com.monikoshop.service;

import com.monikoshop.entity.OtpCode;
import com.monikoshop.entity.OtpPurpose;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.OtpCodeRepository;
import com.monikoshop.security.ClientIp;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * تولید و بررسی کد یک‌بارمصرف ۶ رقمی.
 *  - کد فقط به‌صورت HMAC-SHA256 ذخیره می‌شود و بعد از TTL منقضی است
 *  - هر کد حداکثر چند بار قابل حدس زدن است (شمارش اتمیک در دیتابیس)
 *  - فاصله‌ی بین دو درخواست، سقف در ساعت برای هر شماره و سقف روزانه‌ی کل (جلوگیری از سوءاستفاده‌ی هزینه‌بر)
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    public enum VerifyResult { OK, INVALID, EXPIRED, TOO_MANY_ATTEMPTS }

    /** کد تازه‌ساخته‌شده؛ code فقط برای ارسال پیامک است و هیچ‌جا ذخیره/لاگ نمی‌شود. */
    public record Issued(Long id, String code) { }

    private final OtpCodeRepository repository;

    @Value("${otp.ttl-seconds:180}")
    private long ttlSeconds;

    @Value("${otp.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.max-per-phone-per-hour:5}")
    private int maxPerPhonePerHour;

    @Value("${otp.max-per-day-global:2000}")
    private int maxPerDayGlobal;

    /** راز HMAC. اگر خالی باشد هر بار با ری‌استارت یک راز تصادفی ساخته می‌شود (کدهای در جریان باطل می‌شوند). */
    @Value("${otp.hash-secret:}")
    private String hashSecret;

    private final SecureRandom random = new SecureRandom();
    private byte[] pepper;

    @PostConstruct
    void init() {
        String s = hashSecret == null ? "" : hashSecret.trim();
        if (s.length() < 16) {
            pepper = new byte[32];
            random.nextBytes(pepper);
            log.warn("OTP_HASH_SECRET is missing or too short (<16 chars); a random temporary secret is used.");
        } else {
            pepper = s.getBytes(StandardCharsets.UTF_8);
        }
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public long cooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * یک کد تازه می‌سازد و کدهای قبلیِ همان شماره/هدف را باطل می‌کند.
     * @throws ApiException (429) اگر محدودیت‌ها رد شده باشند
     */
    public Issued create(String phone, OtpPurpose purpose) {
        Instant now = Instant.now();

        if (repository.countByCreatedAtAfter(now.minus(Duration.ofDays(1))) >= maxPerDayGlobal) {
            log.error("Global daily OTP limit reached ({}).", maxPerDayGlobal);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "ارسال پیامک موقتاً محدود شده است. لطفاً بعداً دوباره تلاش کنید");
        }
        Optional<OtpCode> latest = repository.findFirstByPhoneOrderByCreatedAtDesc(phone);
        if (latest.isPresent()) {
            long wait = cooldownSeconds - Duration.between(latest.get().getCreatedAt(), now).getSeconds();
            if (wait > 0) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "لطفاً " + wait + " ثانیه دیگر برای دریافت کد جدید صبر کنید");
            }
        }
        if (repository.countByPhoneAndCreatedAtAfter(phone, now.minus(Duration.ofHours(1))) >= maxPerPhonePerHour) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "تعداد درخواست کد برای این شماره زیاد بوده است. یک ساعت دیگر دوباره تلاش کنید");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        repository.invalidateAll(phone, purpose);
        OtpCode saved = repository.save(OtpCode.builder()
                .phone(phone)
                .purpose(purpose)
                .codeHash(hash(phone, purpose, code))
                .attempts(0)
                .consumed(false)
                .ip(currentIp())
                .createdAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .build());
        return new Issued(saved.getId(), code);
    }

    /** اگر ارسال پیامک شکست خورد، رکورد حذف می‌شود تا کاربر بی‌دلیل در فاصله‌ی انتظار نماند. */
    public void discard(Long id) {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            log.warn("could not discard OTP {}: {}", id, e.getMessage());
        }
    }

    /**
     * کد را بررسی می‌کند. در تراکنش «جدا» اجرا می‌شود تا شمارنده‌ی تلاش حتی وقتی
     * درخواست بیرونی خطا می‌دهد (و rollback می‌شود) ثبت بماند.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerifyResult verify(String phone, OtpPurpose purpose, String code) {
        Optional<OtpCode> found = repository
                .findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(phone, purpose);
        if (found.isEmpty() || code == null) {
            return VerifyResult.INVALID;
        }
        OtpCode otp = found.get();
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            repository.markConsumed(otp.getId());
            return VerifyResult.EXPIRED;
        }
        // اول شمارنده‌ی تلاش اتمیک بالا می‌رود، بعد مقایسه می‌شود
        if (repository.incrementAttempts(otp.getId(), maxAttempts) == 0) {
            repository.markConsumed(otp.getId());
            return VerifyResult.TOO_MANY_ATTEMPTS;
        }
        boolean match = MessageDigest.isEqual(
                hash(phone, purpose, code).getBytes(StandardCharsets.UTF_8),
                otp.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!match) {
            return VerifyResult.INVALID;
        }
        repository.markConsumed(otp.getId());
        return VerifyResult.OK;
    }

    /** رکوردهای قدیمی پاک می‌شوند (محدودیت‌های ساعتی/روزانه فقط به ۲۴ ساعت اخیر نیاز دارند). */
    @Scheduled(cron = "0 40 0 * * *", zone = "${tracking.zone:Asia/Tehran}")
    public void pruneOld() {
        try {
            int n = repository.deleteOlderThan(Instant.now().minus(Duration.ofDays(2)));
            if (n > 0) {
                log.info("pruned {} old OTP rows", n);
            }
        } catch (Exception e) {
            log.warn("OTP prune failed: {}", e.getMessage());
        }
    }

    private String hash(String phone, OtpPurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            byte[] out = mac.doFinal((phone + ":" + purpose.name() + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String currentIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest request = sra.getRequest();
            String ip = ClientIp.of(request);
            return ip.length() > 64 ? ip.substring(0, 64) : ip;
        }
        return null;
    }
}
