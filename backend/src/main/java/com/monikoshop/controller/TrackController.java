package com.monikoshop.controller;

import com.monikoshop.dto.TrackRequest;
import com.monikoshop.dto.VisitStatsResponse;
import com.monikoshop.entity.User;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.tracking.VisitTrackingService;
import com.monikoshop.security.ClientIp;
import com.monikoshop.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TrackController {

    private final VisitTrackingService trackingService;
    private final UserRepository userRepository;

    /** عمومی: مرورگر هر ~۲۰ ثانیه صدا می‌زند. اگر توکن معتبر باشد کاربر، وگرنه مهمان. */
    @PostMapping("/api/track/heartbeat")
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody TrackRequest request, HttpServletRequest http) {
        Long userId = CurrentUser.id();
        trackingService.heartbeat(request.visitorId().toLowerCase(), userId, ClientIp.of(http), ClientIp.userAgent(http));
        return ResponseEntity.noContent().build();
    }

    /** فقط ادمین (مسیر /api/admin/** در SecurityConfig). */
    @GetMapping("/api/admin/visits")
    public ResponseEntity<VisitStatsResponse> stats() {
        return ResponseEntity.ok(trackingService.stats());
    }
}
