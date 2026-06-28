package com.onboarding.ai;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

/**
 * Tiny retry helper for outbound AI provider calls. Retries only <em>transient</em> failures
 * — network/timeouts (RestClient {@link ResourceAccessException}), 5xx responses, and 429
 * rate-limits — with exponential backoff. Permanent failures (auth/4xx other than 429) are
 * rethrown immediately so we don't hammer the provider or mask a config error.
 *
 * Deliberately dependency-free (no spring-retry) to keep the stack small. Callers keep their
 * existing try/catch around the call; this only governs whether the operation is re-attempted.
 */
public final class AiRetry {

    private AiRetry() {}

    /**
     * Runs {@code op}, retrying transient failures up to {@code maxAttempts} total tries with
     * backoff of {@code baseBackoffMs * 2^(attempt-1)} between tries. The last exception is
     * rethrown once attempts are exhausted (or immediately for non-transient failures).
     */
    public static <T> T withRetry(int maxAttempts, long baseBackoffMs, Supplier<T> op) {
        int attempts = Math.max(1, maxAttempts);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return op.get();
            } catch (RuntimeException e) {
                last = e;
                if (!isTransient(e) || attempt == attempts) {
                    throw e;
                }
                sleep(baseBackoffMs * (1L << (attempt - 1)));
            }
        }
        throw last; // unreachable: loop either returns or throws
    }

    /** A failure worth retrying: connection/timeout, 5xx, or 429 rate-limit. */
    static boolean isTransient(Throwable e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e instanceof HttpServerErrorException) {
            return true;
        }
        if (e instanceof HttpClientErrorException ce) {
            return ce.getStatusCode().value() == 429;
        }
        return false;
    }

    private static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while backing off before AI retry", ie);
        }
    }
}
