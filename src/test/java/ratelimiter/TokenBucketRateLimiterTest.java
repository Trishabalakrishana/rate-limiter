package ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    @DisplayName("Allows requests up to capacity and rejects subsequent ones")
    void testCapacityLimit() {
        RateLimiter limiter = new TokenBucketRateLimiter(5, 1.0);

        for (int i = 1; i <= 5; i++) {
            assertTrue(limiter.allowRequest("client-1"), "Request #" + i + " should be allowed");
        }

        assertFalse(limiter.allowRequest("client-1"), "Request #6 should be blocked as bucket is empty");
    }

    @Test
    @DisplayName("Refills tokens over time")
    void testRefillOverTime() throws InterruptedException {
        RateLimiter limiter = new TokenBucketRateLimiter(2, 5.0); // 5 tokens/sec = 1 token every 200ms

        assertTrue(limiter.allowRequest("client-refill"));
        assertTrue(limiter.allowRequest("client-refill"));
        assertFalse(limiter.allowRequest("client-refill"));

        // Wait 350ms to allow at least 1 token to refill
        Thread.sleep(350);

        assertTrue(limiter.allowRequest("client-refill"), "Should allow request after token refills");
    }

    @Test
    @DisplayName("Isolates state across distinct clients")
    void testClientIsolation() {
        RateLimiter limiter = new TokenBucketRateLimiter(2, 1.0);

        assertTrue(limiter.allowRequest("client-A"));
        assertTrue(limiter.allowRequest("client-A"));
        assertFalse(limiter.allowRequest("client-A"), "Client A has exhausted tokens");

        // Client B must still have full quota
        assertTrue(limiter.allowRequest("client-B"), "Client B should be unaffected by Client A");
        assertTrue(limiter.allowRequest("client-B"), "Client B second request should be allowed");
    }

    @Test
    @DisplayName("Handles high concurrency safely without over-granting tokens (Race Condition Test)")
    void testConcurrentRequests() throws InterruptedException {
        int capacity = 10;
        int totalThreads = 30;
        RateLimiter limiter = new TokenBucketRateLimiter(capacity, 0.1); // very slow refill so refill doesn't interfere

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // wait for all threads to align
                    if (limiter.allowRequest("concurrent-client")) {
                        allowedCount.incrementAndGet();
                    } else {
                        blockedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // fire simultaneously
        doneLatch.await();
        executor.shutdown();

        assertEquals(capacity, allowedCount.get(), "Allowed count must match exact capacity under race conditions");
        assertEquals(totalThreads - capacity, blockedCount.get(), "Blocked count must equal remaining threads");
    }

    @Test
    @DisplayName("Throws exception on invalid constructor arguments or empty clientId")
    void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucketRateLimiter(0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucketRateLimiter(5, -1.0));

        RateLimiter limiter = new TokenBucketRateLimiter(5, 1.0);
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest(null));
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest("   "));
    }
}
