package com.monikoshop.exception;

import com.monikoshop.dto.MessageResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handling. Only {@link ApiException} messages are shown to users;
 * everything unexpected is logged on the server and returned as a generic message
 * so internal details (SQL, class names, stack traces) never leak.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        String first = null;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            if (first == null) {
                first = error.getDefaultMessage();
            }
        }
        errors.put("message", first != null ? first : "داده‌های ورودی نامعتبر است");
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<MessageResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "این عملیات با محدودیت پایگاه داده تداخل دارد";
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        String lower = detail == null ? "" : detail.toLowerCase();
        if (lower.contains("email")) {
            message = "این ایمیل قبلاً ثبت شده است";
        } else if (lower.contains("order_items") || lower.contains("products")) {
            message = "امکان حذف وجود ندارد، زیرا این مورد در سفارش‌ها یا محصولات دیگر استفاده شده است";
        }
        log.warn("Data integrity violation: {}", detail);
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    // Bad credentials, disabled/locked accounts: same generic answer so attackers can't tell which case it was.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("ایمیل یا رمز عبور اشتباه است"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("دسترسی مجاز نیست"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class})
    public ResponseEntity<MessageResponse> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("درخواست نامعتبر است"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<MessageResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new MessageResponse("حجم فایل ارسالی بیش از حد مجاز است"));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<MessageResponse> handleErrorResponse(ErrorResponseException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new MessageResponse("درخواست نامعتبر است"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleUnexpected(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("خطای داخلی سرور. لطفاً بعداً دوباره تلاش کنید"));
    }
}
