package ratelimiter;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-threaded simulation demonstrating Token Bucket and Sliding Window Log rate limiters.
 * Simulates multiple concurrent clients with varied traffic profiles (burst vs. steady)
 * and outputs live console feedback and a final side-by-side performance summary.
 */
public class Demo {

    // ANSI color escape codes for terminal output
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    public static void main(String[] args) throws InterruptedException {
        System.out.println(CYAN + BOLD + "==========================================================" + RESET);
        System.out.println(CYAN + BOLD + "          RATE LIMITER CONCURRENT SIMULATION DEMO          " + RESET);
        System.out.println(CYAN + BOLD + "==========================================================" + RESET);

        // Run Token Bucket Scenario
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(5, 2.0); // Capacity 5, refills 2 tokens/sec
        SimulationResult tbResult = runSimulation("Token Bucket (Capacity=5, Refill=2/sec)", tokenBucket);

        Thread.sleep(1000); // brief pause between simulations

        // Run Sliding Window Log Scenario
        RateLimiter slidingWindow = RateLimiterFactory.createSlidingWindow(5, Duration.ofSeconds(2)); // Max 5 per 2s
        SimulationResult swResult = runSimulation("Sliding Window Log (Max=5, Window=2.0s)", slidingWindow);

        // Print comparative summary
        printSummary(tbResult, swResult);
    }

    private static SimulationResult runSimulation(String algorithmName, RateLimiter limiter) throws InterruptedException {
        System.out.println("\n" + YELLOW + BOLD + ">>> Running Scenario with: " + algorithmName + RESET);
        System.out.println("Simulating 2 clients simultaneously:\n" +
                "  - Client-Burst: Fires 8 requests instantly (tests burst capacity)\n" +
                "  - Client-Steady: Fires 5 requests with 350ms delays (tests continuous pacing)");
        System.out.println("----------------------------------------------------------");

        AtomicInteger totalAllowed = new AtomicInteger(0);
        AtomicInteger totalBlocked = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Worker 1: Bursting Client
        executor.submit(() -> {
            String client = "Client-Burst";
            for (int i = 1; i <= 8; i++) {
                boolean allowed = limiter.allowRequest(client);
                recordAndLog(algorithmName, client, i, allowed, totalAllowed, totalBlocked);
                // slight 20ms jitter between burst requests
                sleepMillis(20);
            }
        });

        // Worker 2: Steady Client
        executor.submit(() -> {
            String client = "Client-Steady";
            for (int i = 1; i <= 5; i++) {
                boolean allowed = limiter.allowRequest(client);
                recordAndLog(algorithmName, client, i, allowed, totalAllowed, totalBlocked);
                sleepMillis(350);
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        return new SimulationResult(algorithmName, 13, totalAllowed.get(), totalBlocked.get());
    }

    private static void recordAndLog(String algo, String clientId, int reqNum, boolean allowed,
                                     AtomicInteger totalAllowed, AtomicInteger totalBlocked) {
        if (allowed) {
            totalAllowed.incrementAndGet();
            System.out.printf("[%s] %-14s Request #%-2d -> %s[ALLOWED]%s%n",
                    algo.split(" ")[0], clientId, reqNum, GREEN + BOLD, RESET);
        } else {
            totalBlocked.incrementAndGet();
            System.out.printf("[%s] %-14s Request #%-2d -> %s[BLOCKED - 429 Too Many Requests]%s%n",
                    algo.split(" ")[0], clientId, reqNum, RED + BOLD, RESET);
        }
    }

    private static void printSummary(SimulationResult tb, SimulationResult sw) {
        System.out.println("\n" + CYAN + BOLD + "==========================================================" + RESET);
        System.out.println(CYAN + BOLD + "                  SIMULATION SUMMARY                      " + RESET);
        System.out.println(CYAN + BOLD + "==========================================================" + RESET);
        System.out.printf("%-25s | %-12s | %-10s | %-10s%n", "Algorithm", "Total Reqs", "Allowed", "Blocked");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf("%-25s | %-12d | %-10d | %-10d%n", tb.name.split("\\(")[0].trim(), tb.totalRequests, tb.allowed, tb.blocked);
        System.out.printf("%-25s | %-12d | %-10d | %-10d%n", sw.name.split("\\(")[0].trim(), sw.totalRequests, sw.allowed, sw.blocked);
        System.out.println("----------------------------------------------------------------------");
        System.out.println(BOLD + "Key Architectural Takeaways for Interviews:" + RESET);
        System.out.println("1. " + BOLD + "Token Bucket:" + RESET + " O(1) space complexity. Accommodates bursts when idle, refills smoothly.");
        System.out.println("2. " + BOLD + "Sliding Window Log:" + RESET + " O(M) space complexity. Guarantees zero boundary-burst window violations.");
        System.out.println("3. " + BOLD + "Thread Safety:" + RESET + " Per-client locks eliminate cross-client lock contention under concurrent load.\n");
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record SimulationResult(String name, int totalRequests, int allowed, int blocked) {}
}
