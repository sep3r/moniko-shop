package com.monikoshop.exception;

import com.monikoshop.dto.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralizes error handling so controllers don't need repetitive try/catch blocks,
 * and so unexpected exceptions (validation failures, duplicate-email race conditions,
 * bad credentials) return clean, predictable JSON instead of a default Spring 500/400 page.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Hit both when two requests race past the existsByEmail check and both try
        // to insert the same unique email, and when the admin panel tries to delete
        // a category/product that's still referenced elsewhere (e.g. past orders).
        String message = "این عملیات با محدودیت پایگاه داده تداخل دارد";
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (detail != null && detail.toLowerCase().contains("email")) {
            message = "این ایمیل قبلاً ثبت شده است";
        } else if (detail != null && (detail.toLowerCase().contains("order_items") || detail.toLowerCase().contains("products"))) {
            message = "امکان حذف وجود ندارد، زیرا این مورد در سفارش‌ها یا محصولات دیگر استفاده شده است";
        }
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse("ایمیل یا رمز عبور اشتباه است"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntime(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }
}
