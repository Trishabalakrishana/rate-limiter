package ratelimiter;

/**
 * Common contract for rate limiting implementations.
 */
public interface RateLimiter {
    /**
     * Determines whether a request for the given client ID is allowed under the rate limit policy.
     *
     * @param clientId unique identifier for the client (e.g., user ID, API key, or IP address)
     * @return true if the request is permitted; false if it exceeds the rate limit
     */
    boolean allowRequest(String clientId);
}
