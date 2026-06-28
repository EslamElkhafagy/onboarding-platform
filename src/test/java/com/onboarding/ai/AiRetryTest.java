package com.onboarding.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRetryTest {

    @Test
    void returnsImmediatelyWhenOperationSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        String result = AiRetry.withRetry(3, 0, () -> {
            calls.incrementAndGet();
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    void retriesTransientFailureThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        String result = AiRetry.withRetry(3, 0, () -> {
            if (calls.incrementAndGet() < 3) {
                throw HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                        "boom", null, null, null);
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void retriesRateLimitAndNetworkErrors() {
        assertEquals(true, AiRetry.isTransient(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "429", null, null, null)));
        assertEquals(true, AiRetry.isTransient(new ResourceAccessException("connection reset")));
        assertEquals(true, AiRetry.isTransient(
                HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "502", null, null, null)));
    }

    @Test
    void doesNotRetryNonTransientFailures() {
        AtomicInteger calls = new AtomicInteger();
        HttpClientErrorException unauthorized =
                HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "401", null, null, null);
        HttpClientErrorException thrown = assertThrows(HttpClientErrorException.class, () ->
                AiRetry.withRetry(3, 0, () -> {
                    calls.incrementAndGet();
                    throw unauthorized;
                }));
        assertSame(unauthorized, thrown);
        assertEquals(1, calls.get());
        assertEquals(false, AiRetry.isTransient(unauthorized));
    }

    @Test
    void rethrowsLastExceptionAfterExhaustingAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpServerErrorException.class, () ->
                AiRetry.withRetry(2, 0, () -> {
                    calls.incrementAndGet();
                    throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE,
                            "503", null, null, null);
                }));
        assertEquals(2, calls.get());
    }
}
