package com.monikoshop.service;


import com.monikoshop.audit.AuditEvent;
import com.monikoshop.audit.AuditService;
import com.monikoshop.exception.ApiException;
import com.monikoshop.dto.*;
import com.monikoshop.entity.Role;
import com.monikoshop.entity.User;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.security.JwtUtil;
import com.monikoshop.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String TOO_MANY = "تعداد تلاش‌ها بیش از حد مجاز است. چند دقیقه بعد دوباره تلاش کنید";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("این ایمیل قبلاً ثبت شده است");
        }
        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("رمز عبور بیش از حد طولانی است");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(Set.of(Role.ROLE_USER))
                .enabled(true)
                .lastLoginAt(Instant.now())
                .build();

        user = userRepository.save(user);
        auditService.log(AuditEvent.REGISTER, user.getId(), user.getEmail(), null);


        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();

        String token = jwtUtil.generateToken(userDetails, user.getPassword());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // محدودیت به‌ازای ایمیل (علاوه بر محدودیت به‌ازای IP در RateLimitFilter)
        if (loginAttempts.isBlocked(email)) {
            auditService.log(AuditEvent.LOGIN_BLOCKED, null, email, null);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY);
        }
        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            loginAttempts.recordFailure(email);
            auditService.log(AuditEvent.LOGIN_FAILURE, null, email, "password too long");
            throw new org.springframework.security.authentication.BadCredentialsException("invalid credentials");
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            loginAttempts.recordFailure(email);
            auditService.log(AuditEvent.LOGIN_FAILURE, null, email, null);
            throw ex;
        }
        loginAttempts.clear(email);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));

        // ثبت زمان آخرین ورود موفق
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.log(AuditEvent.LOGIN_SUCCESS, user.getId(), user.getEmail(), null);

        String token = jwtUtil.generateToken(userDetails, user.getPassword());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    /**
     * تغییر رمز برای کاربر واردشده. رمز فعلی باید درست باشد.
     * توکن به هش رمز وابسته است، پس همه‌ی توکن‌های قبلی (روی دستگاه‌های دیگر) باطل می‌شوند
     * و یک توکن تازه برای همین نشست برگردانده می‌شود.
     */
    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));

        String current = request.getCurrentPassword();
        String next = request.getNewPassword();
        String attemptKey = "pw:" + user.getEmail();

        if (loginAttempts.isBlocked(attemptKey)) {
            auditService.log(AuditEvent.PASSWORD_CHANGE_FAILED, user.getId(), user.getEmail(), "blocked");
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY);
        }
        if (current.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(current, user.getPassword())) {
            loginAttempts.recordFailure(attemptKey);
            auditService.log(AuditEvent.PASSWORD_CHANGE_FAILED, user.getId(), user.getEmail(), "wrong current password");
            throw new ApiException("رمز عبور فعلی اشتباه است");
        }
        loginAttempts.clear(attemptKey);
        if (next.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ApiException("رمز عبور بیش از حد طولانی است");
        }
        if (next.equals(current)) {
            throw new ApiException("رمز عبور جدید باید با رمز فعلی متفاوت باشد");
        }

        user.setPassword(passwordEncoder.encode(next));
        user = userRepository.save(user);
        auditService.log(AuditEvent.PASSWORD_CHANGED, user.getId(), user.getEmail(), null);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();

        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userDetails, user.getPassword()))
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }
}
