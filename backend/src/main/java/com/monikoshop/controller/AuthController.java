package com.monikoshop.controller;

import com.monikoshop.dto.*;
import com.monikoshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** ثبت‌نام مرحله‌ی ۱: پیامک کد تأیید به شماره. */
    @PostMapping("/register/otp")
    public ResponseEntity<OtpSentResponse> registerOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(authService.requestRegisterOtp(request.getPhone()));
    }

    /** ثبت‌نام مرحله‌ی ۲: کد + نام + رمز عبور؛ کاربر ساخته و وارد می‌شود. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** ورود فقط با شماره + رمز عبور (بدون پیامک). */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** پاسخ همیشه یکسان است، چه شماره ثبت شده باشد چه نه. */
    @PostMapping("/forgot-password")
    public ResponseEntity<OtpSentResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.requestPasswordResetOtp(request.getPhone()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    /** نیاز به ورود دارد. کد را به شماره‌ی تأییدشده‌ی خود کاربر می‌فرستد. */
    @PostMapping("/change-password/otp")
    public ResponseEntity<OtpSentResponse> changePasswordOtp() {
        return ResponseEntity.ok(authService.requestChangePasswordOtp());
    }

    /** نیاز به ورود دارد (anyRequest().authenticated()). توکن جدید برمی‌گرداند. */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
