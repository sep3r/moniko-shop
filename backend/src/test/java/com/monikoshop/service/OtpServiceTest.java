package com.monikoshop.service;

import com.monikoshop.entity.OtpCode;
import com.monikoshop.entity.OtpPurpose;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.OtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private static final String PHONE = "09123456789";

    private OtpCodeRepository repo;
    private OtpService service;
    private final List<OtpCode> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = mock(OtpCodeRepository.class);
        service = new OtpService(repo);
        ReflectionTestUtils.setField(service, "ttlSeconds", 180L);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "maxPerPhonePerHour", 5);
        ReflectionTestUtils.setField(service, "maxPerDayGlobal", 2000);
        ReflectionTestUtils.setField(service, "hashSecret", "0123456789abcdef-test-secret");
        ReflectionTestUtils.invokeMethod(service, "init");

        when(repo.save(any(OtpCode.class))).thenAnswer(inv -> {
            OtpCode o = inv.getArgument(0);
            o.setId(7L);
            saved.add(o);
            return o;
        });
        when(repo.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.empty());
    }

    @Test
    void createsSixDigitCodeAndStoresOnlyItsHash() {
        OtpService.Issued issued = service.create(PHONE, OtpPurpose.REGISTER);
        assertTrue(issued.code().matches("\\d{6}"));
        OtpCode row = saved.get(0);
        assertNotEquals(issued.code(), row.getCodeHash());
        assertEquals(64, row.getCodeHash().length());
        assertTrue(row.getExpiresAt().isAfter(Instant.now()));
        verify(repo).invalidateAll(PHONE, OtpPurpose.REGISTER);
    }

    @Test
    void correctCodeVerifiesAndIsConsumed() {
        OtpService.Issued issued = service.create(PHONE, OtpPurpose.REGISTER);
        OtpCode row = saved.get(0);
        when(repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(PHONE, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(row));
        when(repo.incrementAttempts(eq(7L), anyInt())).thenReturn(1);

        assertEquals(OtpService.VerifyResult.OK, service.verify(PHONE, OtpPurpose.REGISTER, issued.code()));
        verify(repo).markConsumed(7L);
    }

    @Test
    void wrongCodeIsRejectedAndNotConsumed() {
        service.create(PHONE, OtpPurpose.REGISTER);
        OtpCode row = saved.get(0);
        when(repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(PHONE, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(row));
        when(repo.incrementAttempts(eq(7L), anyInt())).thenReturn(1);

        assertEquals(OtpService.VerifyResult.INVALID, service.verify(PHONE, OtpPurpose.REGISTER, "000000"));
        verify(repo, never()).markConsumed(anyLong());
    }

    @Test
    void codeCannotBeUsedForAnotherPurpose() {
        OtpService.Issued issued = service.create(PHONE, OtpPurpose.REGISTER);
        OtpCode row = saved.get(0);
        when(repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(PHONE, OtpPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(row));
        when(repo.incrementAttempts(eq(7L), anyInt())).thenReturn(1);

        assertEquals(OtpService.VerifyResult.INVALID, service.verify(PHONE, OtpPurpose.RESET_PASSWORD, issued.code()));
    }

    @Test
    void tooManyAttemptsBlocksEvenTheCorrectCode() {
        OtpService.Issued issued = service.create(PHONE, OtpPurpose.REGISTER);
        OtpCode row = saved.get(0);
        when(repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(PHONE, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(row));
        when(repo.incrementAttempts(eq(7L), anyInt())).thenReturn(0);

        assertEquals(OtpService.VerifyResult.TOO_MANY_ATTEMPTS, service.verify(PHONE, OtpPurpose.REGISTER, issued.code()));
    }

    @Test
    void expiredCodeIsRejected() {
        service.create(PHONE, OtpPurpose.REGISTER);
        OtpCode row = saved.get(0);
        row.setExpiresAt(Instant.now().minusSeconds(1));
        when(repo.findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(PHONE, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(row));

        assertEquals(OtpService.VerifyResult.EXPIRED, service.verify(PHONE, OtpPurpose.REGISTER, "123456"));
    }

    @Test
    void resendWithinCooldownIsRejected() {
        OtpCode recent = OtpCode.builder().createdAt(Instant.now().minusSeconds(10)).build();
        when(repo.findFirstByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(recent));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(PHONE, OtpPurpose.REGISTER));
        assertEquals(429, ex.getStatus().value());
    }

    @Test
    void hourlyLimitPerPhoneIsEnforced() {
        when(repo.countByPhoneAndCreatedAtAfter(eq(PHONE), any(Instant.class))).thenReturn(5L);
        assertThrows(ApiException.class, () -> service.create(PHONE, OtpPurpose.REGISTER));
        verify(repo, never()).save(any());
    }

    @Test
    void globalDailyLimitIsEnforced() {
        when(repo.countByCreatedAtAfter(any(Instant.class))).thenReturn(2000L);
        assertThrows(ApiException.class, () -> service.create(PHONE, OtpPurpose.REGISTER));
    }
}
