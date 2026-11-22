package com.monikoshop.tracking;

import com.monikoshop.dto.VisitStatsResponse;
import com.monikoshop.entity.DailyVisitStat;
import com.monikoshop.entity.VisitLog;
import com.monikoshop.entity.VisitorDay;
import com.monikoshop.repository.DailyVisitStatRepository;
import com.monikoshop.repository.VisitLogRepository;
import com.monikoshop.repository.VisitorDayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * شمارش بازدیدکنندگان:
 *  - «همین الان آنلاین»: در حافظه نگه داشته می‌شود (هر مرورگر هر ~۲۰ ثانیه heartbeat می‌فرستد)
 *  - «امروز»: هر بازدیدکننده‌ی یکتا یک ردیف در visitor_day (فقط بار اول هر روز نوشته می‌شود)
 *  - «آخر روز»: عدد نهایی هر روز در daily_visit_stats ذخیره می‌شود
 * علاوه بر شمارش، برای هر «روز + مرورگر + IP» یک ردیف در visit_log (IP و User-Agent مهمان/کاربر) ثبت می‌شود
 * و بعد از VISIT_LOG_RETENTION_DAYS (پیش‌فرض ۹۰ روز) پاک می‌شود.
 * مرز روز بر اساس منطقه‌ی زمانی TRACKING_ZONE است (پیش‌فرض Asia/Tehran).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitTrackingService {

    /** اگر در این بازه heartbeat نیامده باشد، کاربر آنلاین حساب نمی‌شود. */
    private static final long ONLINE_WINDOW_MS = 60_000L;
    private static final int MAX_ONLINE_KEYS = 100_000;
    private static final int HISTORY_DAYS = 30;
    /** ردیف‌های ریز visitor_day بعد از این مدت پاک می‌شوند (عدد نهایی روزها می‌ماند). */
    private static final int RAW_RETENTION_DAYS = 45;

    // سقف‌ها در برابر پر کردن حافظه/دیتابیس با شناسه‌های جعلی (heartbeat عمومی است)
    private static final int MAX_RECORDED_PER_DAY = 300_000;
    /** مرورگر جدید در روز به‌ازای هر IP؛ بالا چون پشت NAT اپراتورهای موبایل هزاران نفر یک IP دارند. */
    private static final int MAX_NEW_VISITORS_PER_IP_PER_DAY = 1_000;
    /** هر مرورگر/IP حداکثر هر ۵ دقیقه یک بار در visit_log به‌روز می‌شود. */
    private static final long LOG_TOUCH_INTERVAL_MS = 5 * 60_000L;

    private final VisitorDayRepository visitorDayRepository;
    private final DailyVisitStatRepository statRepository;
    private final VisitLogRepository visitLogRepository;

    @Value("${tracking.zone:Asia/Tehran}")
    private String zoneId;

    @Value("${VISIT_LOG_RETENTION_DAYS:90}")
    private int logRetentionDays;

    private record Presence(long lastSeen, Long userId) {}

    private final Map<String, Presence> online = new ConcurrentHashMap<>();

    // کش امروز: visitorId -> userId (0 = مهمان). فقط برای جلوگیری از نوشتن/خواندن تکراری DB.
    private final Map<String, Long> recordedToday = new ConcurrentHashMap<>();
    private volatile LocalDate recordedDate;

    // تعداد مرورگر جدید امروز به‌ازای هر IP، و زمان آخرین نوشتن visit_log: "visitorId|ip" -> {lastWriteMs, userMarker}
    private final Map<String, Integer> newVisitorsPerIp = new ConcurrentHashMap<>();
    private final Map<String, long[]> logTouched = new ConcurrentHashMap<>();

    private ZoneId zone() {
        return ZoneId.of(zoneId);
    }

    private LocalDate today() {
        return LocalDate.now(zone());
    }

    /** با هر heartbeat صدا زده می‌شود. userId برای مهمان null است. */
    public void heartbeat(String visitorId, Long userId, String ip, String userAgent) {
        long now = System.currentTimeMillis();
        String safeIp = (ip == null || ip.isBlank()) ? "unknown" : ip;

        LocalDate day = today();
        if (!day.equals(recordedDate)) {
            synchronized (this) {
                if (!day.equals(recordedDate)) {
                    recordedToday.clear();
                    newVisitorsPerIp.clear();
                    logTouched.clear();
                    recordedDate = day;
                }
            }
        }

        long marker = userId == null ? 0L : userId;
        Long prev = recordedToday.get(visitorId);

        if (prev == null) {
            // مرورگر جدید امروز: سقف کلی و سقف به‌ازای IP
            if (recordedToday.size() >= MAX_RECORDED_PER_DAY) {
                return;
            }
            if (newVisitorsPerIp.merge(safeIp, 1, Integer::sum) > MAX_NEW_VISITORS_PER_IP_PER_DAY) {
                return;
            }
        }

        if (online.size() >= MAX_ONLINE_KEYS) {
            online.entrySet().removeIf(e -> now - e.getValue().lastSeen() > ONLINE_WINDOW_MS);
            if (online.size() >= MAX_ONLINE_KEYS && !online.containsKey(visitorId)) {
                return;
            }
        }
        online.put(visitorId, new Presence(now, userId));

        boolean needsDayRow = prev == null || (prev == 0L && marker != 0L);
        if (needsDayRow) {
            persist(day, visitorId, userId);
            recordedToday.put(visitorId, marker);
        }

        logVisit(day, visitorId, userId, marker, safeIp, userAgent, now);
    }

    /** ثبت/به‌روزرسانی ردیف visit_log (IP و User-Agent)؛ با محدودیت زمانی تا دیتابیس هر ۲۰ ثانیه نوشته نشود. */
    private void logVisit(LocalDate day, String visitorId, Long userId, long marker,
                          String ip, String userAgent, long now) {
        String key = visitorId + "|" + ip;
        long[] state = logTouched.get(key);
        if (state == null && logTouched.size() >= MAX_RECORDED_PER_DAY) {
            return;
        }
        boolean userJustLoggedIn = state != null && state[1] == 0L && marker != 0L;
        if (state != null && !userJustLoggedIn && now - state[0] < LOG_TOUCH_INTERVAL_MS) {
            return;
        }
        try {
            Instant ts = Instant.now();
            VisitLog row = visitLogRepository.findByVisitDateAndVisitorIdAndIp(day, visitorId, ip).orElse(null);
            if (row == null) {
                visitLogRepository.save(VisitLog.builder()
                        .visitDate(day)
                        .visitorId(visitorId)
                        .userId(userId)
                        .ip(ip.length() > 64 ? ip.substring(0, 64) : ip)
                        .userAgent(userAgent)
                        .firstSeen(ts)
                        .lastSeen(ts)
                        .build());
            } else {
                row.setLastSeen(ts);
                if (row.getUserId() == null && userId != null) {
                    row.setUserId(userId);
                }
                visitLogRepository.save(row);
            }
            logTouched.put(key, new long[]{now, marker});
        } catch (DataIntegrityViolationException ignored) {
            // دو درخواست هم‌زمان برای یک مرورگر؛ مشکلی نیست
            logTouched.put(key, new long[]{now, marker});
        } catch (Exception e) {
            log.warn("visit log write failed: {}", e.getMessage());
        }
    }

    private void persist(LocalDate day, String visitorId, Long userId) {
        try {
            VisitorDay row = visitorDayRepository.findByVisitDateAndVisitorId(day, visitorId).orElse(null);
            if (row == null) {
                visitorDayRepository.save(VisitorDay.builder()
                        .visitDate(day)
                        .visitorId(visitorId)
                        .userId(userId)
                        .firstSeen(Instant.now())
                        .build());
            } else if (row.getUserId() == null && userId != null) {
                row.setUserId(userId); // مهمانِ امروز، بعداً لاگین کرد → کاربر حساب می‌شود
                visitorDayRepository.save(row);
            }
        } catch (DataIntegrityViolationException ignored) {
            // دو درخواست هم‌زمان برای یک بازدیدکننده؛ مشکلی نیست
        } catch (Exception e) {
            log.warn("visit tracking write failed: {}", e.getMessage());
        }
    }

    public VisitStatsResponse stats() {
        long now = System.currentTimeMillis();
        long total = 0, users = 0;
        for (Presence p : online.values()) {
            if (now - p.lastSeen() <= ONLINE_WINDOW_MS) {
                total++;
                if (p.userId() != null) users++;
            }
        }
        var onlineNow = new VisitStatsResponse.Online(total, users, total - users);

        LocalDate today = today();
        long dayTotal = visitorDayRepository.countByVisitDate(today);
        long dayUsers = visitorDayRepository.countByVisitDateAndUserIdIsNotNull(today);
        var todayStats = new VisitStatsResponse.Day(today, dayTotal, dayUsers, dayTotal - dayUsers);

        List<VisitStatsResponse.Day> history = statRepository
                .findByStatDateGreaterThanEqualOrderByStatDateDesc(today.minusDays(HISTORY_DAYS))
                .stream()
                .map(s -> new VisitStatsResponse.Day(s.getStatDate(), s.getTotalVisitors(),
                        s.getUserVisitors(), s.getGuestVisitors()))
                .toList();

        return new VisitStatsResponse(onlineNow, todayStats, history);
    }

    // ---------- ذخیره‌ی عدد نهایی هر روز ----------

    /** هر شب ۰۰:۰۵ آمار دیروز (و هر روز ذخیره‌نشده‌ی قبلی) را در daily_visit_stats می‌نویسد. */
    @Scheduled(cron = "0 5 0 * * *", zone = "${tracking.zone:Asia/Tehran}")
    public void finalizePastDays() {
        LocalDate today = today();
        for (LocalDate day : visitorDayRepository.findDistinctDatesBefore(today)) {
            saveDayStat(day);
        }
        int deleted = visitorDayRepository.deleteOlderThan(today.minusDays(RAW_RETENTION_DAYS));
        if (deleted > 0) log.info("visit tracking: pruned {} old raw rows", deleted);
        int logsDeleted = visitLogRepository.deleteOlderThan(today.minusDays(Math.max(7, logRetentionDays)));
        if (logsDeleted > 0) log.info("visit tracking: pruned {} old visit_log rows", logsDeleted);
    }

    /** اگر سرور نیمه‌شب خاموش بوده، موقع بالا آمدن روزهای جاافتاده جبران می‌شوند. */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        try {
            finalizePastDays();
        } catch (Exception e) {
            log.warn("visit stats catch-up failed: {}", e.getMessage());
        }
    }

    private void saveDayStat(LocalDate day) {
        long total = visitorDayRepository.countByVisitDate(day);
        long users = visitorDayRepository.countByVisitDateAndUserIdIsNotNull(day);
        DailyVisitStat stat = statRepository.findById(day).orElseGet(DailyVisitStat::new);
        stat.setStatDate(day);
        stat.setTotalVisitors(total);
        stat.setUserVisitors(users);
        stat.setGuestVisitors(total - users);
        stat.setSavedAt(Instant.now());
        statRepository.save(stat);
        log.info("visit stats saved for {}: total={}, users={}, guests={}", day, total, users, total - users);
    }
}
