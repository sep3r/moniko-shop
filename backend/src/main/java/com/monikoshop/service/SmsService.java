package com.monikoshop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * ارسال پیامک تأیید با وب‌سرویس کاوه‌نگار (Verify Lookup).
 * همان endpointی را صدا می‌زند که SDK نود (kavenegar.js) صدا می‌زند:
 *   POST https://api.kavenegar.com/v1/{API_KEY}/verify/lookup.json
 *   پارامترها: receptor, token, template
 * قالب (template) باید قبلاً در پنل کاوه‌نگار ساخته و تأیید شده باشد.
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${kavenegar.api-key:}")
    private String apiKey;

    /** نام قالب تأییدیه در پنل کاوه‌نگار؛ متن قالب باید شامل %token باشد. */
    @Value("${kavenegar.template:}")
    private String template;

    @Value("${kavenegar.base-url:https://api.kavenegar.com}")
    private String baseUrl;

    /** فقط برای توسعه‌ی محلی: اگر کاوه‌نگار تنظیم نشده باشد، کد در لاگ چاپ می‌شود. در production خاموش بماند. */
    @Value("${kavenegar.dev-log-codes:false}")
    private boolean devLogCodes;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && template != null && !template.isBlank();
    }

    /** @return true اگر کاوه‌نگار پیامک را پذیرفته باشد (یا در حالت dev کد لاگ شده باشد) */
    public boolean sendOtp(String phone, String code) {
        if (!isConfigured()) {
            if (devLogCodes) {
                log.warn("[DEV] Kavenegar not configured; OTP for {} is {}", phone, code);
                return true;
            }
            log.error("Kavenegar is not configured (KAVENEGAR_API_KEY / KAVENEGAR_TEMPLATE); OTP SMS was not sent.");
            return false;
        }
        try {
            String body = "receptor=" + enc(phone) + "&token=" + enc(code) + "&template=" + enc(template.trim());
            String base = baseUrl.trim().replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/v1/" + apiKey.trim() + "/verify/lookup.json"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            JsonNode ret = mapper.readTree(response.body()).path("return");
            int status = ret.path("status").asInt(response.statusCode());
            if (status == 200) {
                return true;
            }
            // آدرس درخواست (که کلید API در آن است) هرگز لاگ نمی‌شود
            log.error("Kavenegar rejected the OTP SMS: status={} message={}", status, ret.path("message").asText(""));
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kavenegar request interrupted");
            return false;
        } catch (Exception e) {
            log.error("Kavenegar request failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /** در پس‌زمینه می‌فرستد تا زمان پاسخ نشان ندهد شماره‌ای ثبت شده است یا نه (فراموشی رمز). */
    public void sendOtpAsync(String phone, String code) {
        CompletableFuture.runAsync(() -> sendOtp(phone, code));
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
