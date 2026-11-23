package com.monikoshop.service;

import com.monikoshop.audit.AuditEvent;
import com.monikoshop.audit.AuditService;
import com.monikoshop.dto.*;
import com.monikoshop.entity.OtpPurpose;
import com.monikoshop.entity.Role;
import com.monikoshop.entity.User;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.security.CurrentUser;
import com.monikoshop.security.JwtUtil;
import com.monikoshop.security.LoginAttemptService;
import com.monikoshop.security.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttempts;
    private final AuditService auditService;
    private final OtpService otpService;
    private final SmsService smsService;

    private static final String TOO_MANY = "تعداد تلاش‌ها بیش از حد مجاز است. چند دقیقه بعد دوباره تلاش کنید";
    private static final String SMS_UNAVAILABLE = "ارسال پیامک در حال حاضر ممکن نیست. چند دقیقه بعد دوباره تلاش کنید";
    private static final String PASSWORD_TOO_LONG = "رمز عبور بیش از حد طولانی است";

    // ------------------------------------------------------------------ ثبت‌نام

    /** مرحله‌ی ۱ ثبت‌نام: ارسال کد پیامکی به شماره. */
    public OtpSentResponse requestRegisterOtp(String rawPhone) {
        String phone = requirePhone(rawPhone);
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("این شماره موبایل قبلاً ثبت‌نام کرده است. وارد شوید یا رمز عبور را بازیابی کنید");
        }
        OtpService.Issued issued = otpService.create(phone, OtpPurpose.REGISTER);
        if (!smsService.sendOtp(phone, issued.code())) {
            otpService.discard(issued.id());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, SMS_UNAVAILABLE);
        }
        auditService.log(AuditEvent.OTP_SENT, null, phone, "REGISTER");
        return sentResponse("کد تأیید به شماره " + phone + " پیامک شد");
    }

    /** مرحله‌ی ۲ ثبت‌نام: بررسی کد و ساخت حساب؛ کاربر همان لحظه وارد می‌شود. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String phone = requirePhone(request.getPhone());
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(PASSWORD_TOO_LONG);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("این شماره موبایل قبلاً ثبت‌نام کرده است");
        }
        requireValidOtp(phone, OtpPurpose.REGISTER, request.getCode(), null);

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phone(phone)
                .phoneVerified(true)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .enabled(true)
                .lastLoginAt(Instant.now())
                .build();
        user = userRepository.save(user);
        auditService.log(AuditEvent.REGISTER, user.getId(), phone, null);
        return authResponse(user, Boolean.TRUE.equals(request.getRememberMe()));
    }

    // ------------------------------------------------------------------ ورود (رمز عبور)

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String raw = request.getUsername().trim();
        String phone = PhoneNumbers.normalize(raw);
        String key = phone != null ? phone : raw.toLowerCase();

        // محدودیت به‌ازای شماره/ایمیل (علاوه بر محدودیت به‌ازای IP در RateLimitFilter)
        if (loginAttempts.isBlocked(key)) {
            auditService.log(AuditEvent.LOGIN_BLOCKED, null, key, null);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY);
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            loginAttempts.recordFailure(key);
            auditService.log(AuditEvent.LOGIN_FAILURE, null, key, "password too long");
            throw new BadCredentialsException("invalid credentials");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(key, request.getPassword()));
        } catch (AuthenticationException ex) {
            loginAttempts.recordFailure(key);
            auditService.log(AuditEvent.LOGIN_FAILURE, null, key, null);
            throw ex;
        }
        loginAttempts.clear(key);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findById(Long.valueOf(userDetails.getUsername()))
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        boolean remember = Boolean.TRUE.equals(request.getRememberMe());
        auditService.log(AuditEvent.LOGIN_SUCCESS, user.getId(), user.contactLabel(), remember ? "remember-me" : null);
        return authResponse(user, remember);
    }

    // ------------------------------------------------------------------ فراموشی رمز (پیامک)

    /**
     * همیشه همان پاسخ را می‌دهد، چه شماره ثبت شده باشد چه نه (جلوگیری از شناسایی شماره‌های ثبت‌شده).
     * پیامک فقط برای حساب فعال با شماره‌ی تأییدشده فرستاده می‌شود.
     */
    public OtpSentResponse requestPasswordResetOtp(String rawPhone) {
        String phone = requirePhone(rawPhone);
        auditService.log(AuditEvent.PASSWORD_RESET_REQUESTED, null, phone, null);
        userRepository.findByPhone(phone)
                .filter(u -> u.isEnabled() && u.isPhoneVerified())
                .ifPresent(u -> {
                    try {
                        OtpService.Issued issued = otpService.create(phone, OtpPurpose.RESET_PASSWORD);
                        smsService.sendOtpAsync(phone, issued.code());
                        auditService.log(AuditEvent.OTP_SENT, u.getId(), phone, "RESET_PASSWORD");
                    } catch (ApiException ignored) {
                        // محدودیت تعداد: بیرون فرقی با حالت عادی دیده نمی‌شود
                    }
                });
        return sentResponse("اگر این شماره در سیستم ثبت شده باشد، کد تأیید برای آن پیامک می‌شود");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String phone = requirePhone(request.getPhone());
        User user = userRepository.findByPhone(phone)
                .filter(u -> u.isEnabled() && u.isPhoneVerified())
                .orElseThrow(() -> new ApiException("کد تأیید اشتباه است"));
        if (request.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(PASSWORD_TOO_LONG);
        }
        requireValidOtp(phone, OtpPurpose.RESET_PASSWORD, request.getCode(), user.getId());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        loginAttempts.clear(phone);
        auditService.log(AuditEvent.PASSWORD_RESET_DONE, user.getId(), phone, null);
        return new MessageResponse("رمز عبور با موفقیت تغییر کرد. اکنون می‌توانید وارد شوید");
    }

    // ------------------------------------------------------------------ تغییر رمز (کاربر واردشده)

    /** ارسال کد به شماره‌ی تأییدشده‌ی خودِ کاربر. */
    public OtpSentResponse requestChangePasswordOtp() {
        User user = currentUser();
        if (!hasVerifiedPhone(user)) {
            throw new ApiException("برای این حساب شماره‌ی تأییدشده‌ای ثبت نشده است");
        }
        OtpService.Issued issued = otpService.create(user.getPhone(), OtpPurpose.CHANGE_PASSWORD);
        if (!smsService.sendOtp(user.getPhone(), issued.code())) {
            otpService.discard(issued.id());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, SMS_UNAVAILABLE);
        }
        auditService.log(AuditEvent.OTP_SENT, user.getId(), user.getPhone(), "CHANGE_PASSWORD");
        return sentResponse("کد تأیید به شماره " + user.getPhone() + " پیامک شد");
    }

    /**
     * تغییر رمز: رمز فعلی + (برای حساب دارای شماره‌ی تأییدشده) کد پیامکی.
     * توکن به هش رمز وابسته است، پس توکن‌های قبلی (دستگاه‌های دیگر) باطل می‌شوند
     * و یک توکن تازه برای همین نشست برگردانده می‌شود.
     */
    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        String current = request.getCurrentPassword();
        String next = request.getNewPassword();
        String attemptKey = "pw:" + user.getId();

        if (loginAttempts.isBlocked(attemptKey)) {
            auditService.log(AuditEvent.PASSWORD_CHANGE_FAILED, user.getId(), user.contactLabel(), "blocked");
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY);
        }
        if (current.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(current, user.getPassword())) {
            loginAttempts.recordFailure(attemptKey);
            auditService.log(AuditEvent.PASSWORD_CHANGE_FAILED, user.getId(), user.contactLabel(), "wrong current password");
            throw new ApiException("رمز عبور فعلی اشتباه است");
        }
        loginAttempts.clear(attemptKey);
        if (next.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(PASSWORD_TOO_LONG);
        }
        if (next.equals(current)) {
            throw new ApiException("رمز عبور جدید باید با رمز فعلی متفاوت باشد");
        }
        if (hasVerifiedPhone(user)) {
            requireValidOtp(user.getPhone(), OtpPurpose.CHANGE_PASSWORD, request.getCode(), user.getId());
        }

        user.setPassword(passwordEncoder.encode(next));
        user = userRepository.save(user);
        auditService.log(AuditEvent.PASSWORD_CHANGED, user.getId(), user.contactLabel(), null);
        return authResponse(user, Boolean.TRUE.equals(request.getRememberMe()));
    }

    public UserResponse getCurrentUser() {
        User user = currentUser();
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    // ------------------------------------------------------------------ کمکی

    private User currentUser() {
        Long id = CurrentUser.id();
        if (id == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ابتدا وارد شوید");
        }
        return userRepository.findById(id).orElseThrow(() -> new ApiException("کاربر یافت نشد"));
    }

    private static boolean hasVerifiedPhone(User user) {
        return user.isPhoneVerified() && user.getPhone() != null && !user.getPhone().isBlank();
    }

    private static String requirePhone(String raw) {
        String phone = PhoneNumbers.normalize(raw);
        if (phone == null) {
            throw new ApiException("شماره موبایل معتبر نیست. نمونه: 09123456789");
        }
        return phone;
    }

    private void requireValidOtp(String phone, OtpPurpose purpose, String code, Long userId) {
        if (code == null || code.isBlank()) {
            throw new ApiException("کد تأیید الزامی است");
        }
        OtpService.VerifyResult result = otpService.verify(phone, purpose, PhoneNumbers.digitsOnly(code));
        if (result == OtpService.VerifyResult.OK) {
            return;
        }
        auditService.log(AuditEvent.OTP_FAILED, userId, phone, purpose + ":" + result);
        if (result == OtpService.VerifyResult.EXPIRED) {
            throw new ApiException("کد تأیید منقضی شده است. کد جدید دریافت کنید");
        }
        if (result == OtpService.VerifyResult.TOO_MANY_ATTEMPTS) {
            throw new ApiException("تعداد تلاش‌های اشتباه زیاد بود. کد جدید دریافت کنید");
        }
        throw new ApiException("کد تأیید اشتباه است");
    }

    private OtpSentResponse sentResponse(String message) {
        return new OtpSentResponse(message, otpService.ttlSeconds(), otpService.cooldownSeconds());
    }

    /** توکن به id کاربر (subject) و هش رمز فعلی وابسته است. */
    private AuthResponse authResponse(User user, boolean remember) {
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(details, user.getPassword(), remember))
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }
}
