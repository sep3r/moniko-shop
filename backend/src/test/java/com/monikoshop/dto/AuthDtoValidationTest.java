package com.monikoshop.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** الگوهای regex در DTOها باید معتبر باشند (یک regex خراب فقط موقع اجرا خطای 500 می‌دهد). */
class AuthDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegisterRequest register(String code, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Sara Test");
        r.setPhone("09123456789");
        r.setCode(code);
        r.setPassword(password);
        return r;
    }

    @Test
    void validRegisterRequestPasses() {
        assertTrue(validator.validate(register("123456", "abc12345")).isEmpty());
    }

    @Test
    void persianDigitsAreAcceptedInCode() {
        assertTrue(validator.validate(register("\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6", "abc12345")).isEmpty());
    }

    @Test
    void badCodeAndWeakPasswordAreRejected() {
        assertEquals(1, validator.validate(register("12345", "abc12345")).size());
        assertEquals(1, validator.validate(register("123456", "abcdefgh")).size());
        assertEquals(1, validator.validate(register("123456", "12345678")).size());
    }

    @Test
    void resetPasswordRequestValidates() {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setPhone("09123456789");
        r.setCode("123456");
        r.setNewPassword("abc12345");
        assertTrue(validator.validate(r).isEmpty());
        r.setCode("12a456");
        assertEquals(1, validator.validate(r).size());
    }

    @Test
    void changePasswordCodeIsOptionalButMustBeSixDigits() {
        ChangePasswordRequest r = new ChangePasswordRequest();
        r.setCurrentPassword("old12345");
        r.setNewPassword("new12345");
        assertTrue(validator.validate(r).isEmpty());
        r.setCode("123456");
        assertTrue(validator.validate(r).isEmpty());
        r.setCode("1234");
        assertEquals(1, validator.validate(r).size());
    }
}
