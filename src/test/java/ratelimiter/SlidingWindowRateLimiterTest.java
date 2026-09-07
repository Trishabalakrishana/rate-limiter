package ratelimiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    @Test
    @DisplayName("Allows requests up to limit and blocks subsequent requests within window")
    void testLimitWithinWindow() {
        RateLimiter limiter = new SlidingWindowRateLimiter(3, Duration.ofSeconds(1));

        assertTrue(limiter.allowRequest("client-1"), "Request 1 allowed");
        assertTrue(limiter.allowRequest("client-1"), "Request 2 allowed");
        assertTrue(limiter.allowRequest("client-1"), "Request 3 allowed");
        assertFalse(limiter.allowRequest("client-1"), "Request 4 should be blocked");
    }

    @Test
    @DisplayName("Evicts expired timestamps and permits new requests after window elapses")
    void testSlidingWindowEviction() throws InterruptedException {
        long windowMillis = 300;
        RateLimiter limiter = new SlidingWindowRateLimiter(2, windowMillis);

        assertTrue(limiter.allowRequest("client-evict"));
        assertTrue(limiter.allowRequest("client-evict"));
        assertFalse(limiter.allowRequest("client-evict"), "Exceeded window limit");

        // Wait for current requests to slide out of the window
        Thread.sleep(windowMillis + 50);

        assertTrue(limiter.allowRequest("client-evict"), "New request allowed as older timestamps expired");
        assertTrue(limiter.allowRequest("client-evict"), "Second request allowed in new window");
    }

    @Test
    @DisplayName("Maintains independent sliding windows for separate clients")
    void testClientIsolation() {
        RateLimiter limiter = new SlidingWindowRateLimiter(1, Duration.ofSeconds(1));

        assertTrue(limiter.allowRequest("user-alpha"));
        assertFalse(limiter.allowRequest("user-alpha"), "user-alpha reached limit");

        // user-beta should not be impacted
        assertTrue(limiter.allowRequest("user-beta"), "user-beta should be allowed");
        assertFalse(limiter.allowRequest("user-beta"), "user-beta reached limit");
    }

    @Test
    @DisplayName("Ensures exact limit under concurrent multi-threaded contention (Race Condition Test)")
    void testConcurrentRequests() throws InterruptedException {
        int limit = 7;
        int totalThreads = 25;
        RateLimiter limiter = new SlidingWindowRateLimiter(limit, Duration.ofSeconds(10));

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
                    startLatch.await();
                    if (limiter.allowRequest("contended-client")) {
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
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(limit, allowedCount.get(), "Must allow exactly the limit under high race contention");
        assertEquals(totalThreads - limit, blockedCount.get(), "Must block exactly the surplus requests");
    }

    @Test
    @DisplayName("Throws exception on invalid configurations")
    void testInvalidConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(5, Duration.ofMillis(-100)));
        assertThrows(IllegalArgumentException.class, () -> new SlidingWindowRateLimiter(5, Duration.ZERO));

        RateLimiter limiter = new SlidingWindowRateLimiter(5, 1000);
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest(null));
        assertThrows(IllegalArgumentException.class, () -> limiter.allowRequest(""));
    }
}
