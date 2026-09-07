package ratelimiter;

import java.time.Duration;

/**
 * Factory class for constructing RateLimiter instances.
 * Encapsulates instantiation logic following the Factory Pattern.
 */
public final class RateLimiterFactory {

    private RateLimiterFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a new TokenBucketRateLimiter.
     *
     * @param capacity maximum burst capacity of tokens
     * @param refillTokensPerSecond refill rate in tokens per second
     * @return configured RateLimiter
     */
    public static RateLimiter createTokenBucket(long capacity, double refillTokensPerSecond) {
        return new TokenBucketRateLimiter(capacity, refillTokensPerSecond);
    }

    /**
     * Creates a new SlidingWindowRateLimiter.
     *
     * @param maxRequests maximum allowed requests per sliding window
     * @param windowDuration time window duration
     * @return configured RateLimiter
     */
    public static RateLimiter createSlidingWindow(int maxRequests, Duration windowDuration) {
        return new SlidingWindowRateLimiter(maxRequests, windowDuration);
    }

    /**
     * Creates a new SlidingWindowRateLimiter with millisecond window duration.
     *
     * @param maxRequests maximum allowed requests per sliding window
     * @param windowDurationMillis window duration in milliseconds
     * @return configured RateLimiter
     */
    public static RateLimiter createSlidingWindow(int maxRequests, long windowDurationMillis) {
        return new SlidingWindowRateLimiter(maxRequests, windowDurationMillis);
    }
}
