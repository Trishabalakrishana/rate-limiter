package ratelimiter;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log Rate Limiter.
 *
 * <p>Tracks exact timestamps of each request per client in a double-ended queue (Deque).
 * When a new request arrives, timestamps outside the current rolling window (older than
 * {@code now - windowDuration}) are evicted from the front. If the count of remaining timestamps
 * is under the threshold, the new timestamp is logged and the request is permitted.
 *
 * <p>Thread safety: Maintained via {@link ConcurrentHashMap} for client isolation, with synchronization
 * around individual client log queues ensuring atomic eviction and insertion under concurrent requests.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final long windowDurationMillis;
    private final ConcurrentHashMap<String, SlidingWindow> clientWindows = new ConcurrentHashMap<>();

    /**
     * Constructs a SlidingWindowRateLimiter.
     *
     * @param maxRequests maximum number of requests allowed within the window
     * @param windowDuration time duration of the sliding window
     */
    public SlidingWindowRateLimiter(int maxRequests, Duration windowDuration) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be greater than 0");
        }
        if (windowDuration == null || windowDuration.isNegative() || windowDuration.isZero()) {
            throw new IllegalArgumentException("windowDuration must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowDurationMillis = windowDuration.toMillis();
    }

    /**
     * Overloaded constructor accepting milliseconds directly.
     */
    public SlidingWindowRateLimiter(int maxRequests, long windowDurationMillis) {
        this(maxRequests, Duration.ofMillis(windowDurationMillis));
    }

    @Override
    public boolean allowRequest(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId cannot be null or empty");
        }
        SlidingWindow window = clientWindows.computeIfAbsent(
                clientId,
                k -> new SlidingWindow(maxRequests, windowDurationMillis)
        );
        return window.tryConsume();
    }

    /**
     * Returns the number of logged requests in the active window for a client.
     */
    public int getRequestCount(String clientId) {
        SlidingWindow window = clientWindows.get(clientId);
        return window != null ? window.getActiveCount() : 0;
    }

    /**
     * Internal state representation for a client's timestamp log.
     */
    private static class SlidingWindow {
        private final int limit;
        private final long durationMillis;
        private final Deque<Long> requestTimestamps = new ArrayDeque<>();

        SlidingWindow(int limit, long durationMillis) {
            this.limit = limit;
            this.durationMillis = durationMillis;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            evictExpired(now);

            if (requestTimestamps.size() < limit) {
                requestTimestamps.addLast(now);
                return true;
            }
            return false;
        }

        synchronized int getActiveCount() {
            evictExpired(System.currentTimeMillis());
            return requestTimestamps.size();
        }

        private void evictExpired(long currentTime) {
            long boundary = currentTime - durationMillis;
            while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() <= boundary) {
                requestTimestamps.pollFirst();
            }
        }
    }
}
