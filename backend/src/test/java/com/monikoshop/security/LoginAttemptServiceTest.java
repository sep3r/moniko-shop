package com.monikoshop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private static final long T0 = 1_000_000L;

    @Test
    void blocksAfterTenFailuresAndNotBefore() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            s.recordFailure("a@b.com", T0 + i);
        }
        assertFalse(s.isBlocked("a@b.com", T0 + 100));
        s.recordFailure("a@b.com", T0 + 101);
        assertTrue(s.isBlocked("a@b.com", T0 + 102));
    }

    @Test
    void blockExpiresWhenTheWindowPasses() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            s.recordFailure("a@b.com", T0);
        }
        assertTrue(s.isBlocked("a@b.com", T0 + 1000));
        assertFalse(s.isBlocked("a@b.com", T0 + LoginAttemptService.WINDOW_MS + 1));
    }

    @Test
    void successfulLoginClearsTheCounterAndKeysAreCaseInsensitive() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            s.recordFailure("A@B.com ", T0);
        }
        assertTrue(s.isBlocked("a@b.com", T0 + 1));
        s.clear("a@b.com");
        assertFalse(s.isBlocked("a@b.com", T0 + 2));
    }

    @Test
    void differentKeysDoNotAffectEachOther() {
        LoginAttemptService s = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            s.recordFailure("victim@x.com", T0);
        }
        assertFalse(s.isBlocked("other@x.com", T0 + 1));
    }
}
