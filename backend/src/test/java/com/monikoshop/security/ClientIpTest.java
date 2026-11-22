package com.monikoshop.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpTest {

    @Test
    void usesTheLastForwardedAddressWhichTheProxyAppended() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getHeader("X-Forwarded-For")).thenReturn("6.6.6.6, 203.0.113.9");
        when(r.getRemoteAddr()).thenReturn("10.0.0.1");
        assertEquals("203.0.113.9", ClientIp.of(r));
    }

    @Test
    void fallsBackToRemoteAddress() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRemoteAddr()).thenReturn("198.51.100.7");
        assertEquals("198.51.100.7", ClientIp.of(r));
    }

    @Test
    void userAgentIsTrimmedAndNewlinesRemoved() {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getHeader("User-Agent")).thenReturn("Mozilla\r\nInjected " + "x".repeat(400));
        String ua = ClientIp.userAgent(r);
        assertEquals(255, ua.length());
        assertEquals(-1, ua.indexOf('\n'));
    }

    @Test
    void missingUserAgentIsNull() {
        assertNull(ClientIp.userAgent(mock(HttpServletRequest.class)));
    }
}
