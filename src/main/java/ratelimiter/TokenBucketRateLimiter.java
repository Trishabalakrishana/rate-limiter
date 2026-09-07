package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket Rate Limiter.
 *
 * <p>Each client has a bucket of tokens with a fixed maximum capacity that refills continuously
 * at a specified rate (tokens per second). A request consumes one token. If the bucket is empty,
 * the request is rejected.
 *
 * <p>Thread safety: Per-client instances are maintained in a {@link ConcurrentHashMap}. Mutations
 * on each individual client's bucket are guarded by an intrinsic lock, ensuring safe concurrent access
 * without causing lock contention between distinct clients.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final double refillTokensPerMillis;
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

    /**
     * Constructs a TokenBucketRateLimiter.
     *
     * @param capacity maximum number of tokens a bucket can hold (burst limit)
     * @param refillTokensPerSecond rate at which tokens refill per second
     */
    public TokenBucketRateLimiter(long capacity, double refillTokensPerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("Refill rate must be greater than 0");
        }
        this.capacity = capacity;
        this.refillTokensPerMillis = refillTokensPerSecond / 1000.0;
    }

    @Override
    public boolean allowRequest(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId cannot be null or empty");
        }
        TokenBucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new TokenBucket(capacity, refillTokensPerMillis));
        return bucket.tryConsume();
    }

    /**
     * Returns current available tokens for a specific client (useful for inspection/testing).
     */
    public double getAvailableTokens(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);
        return bucket != null ? bucket.getTokens() : capacity;
    }

    /**
     * Internal state representation for an individual client's bucket.
     */
    private static class TokenBucket {
        private final double maxCapacity;
        private final double refillTokensPerMillis;
        private double currentTokens;
        private long lastRefillTimestamp;

        TokenBucket(double capacity, double refillTokensPerMillis) {
            this.maxCapacity = capacity;
            this.refillTokensPerMillis = refillTokensPerMillis;
            this.currentTokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized double getTokens() {
            refill();
            return currentTokens;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = Math.max(0, now - lastRefillTimestamp);
            if (elapsed > 0) {
                currentTokens = Math.min(maxCapacity, currentTokens + (elapsed * refillTokensPerMillis));
                lastRefillTimestamp = now;
            }
        }
    }
}
