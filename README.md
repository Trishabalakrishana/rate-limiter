# Thread-Safe Java Rate Limiter Library

![Java 21](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5.10-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Concurrency](https://img.shields.io/badge/Concurrency-Thread--Safe-blue?style=for-the-badge)

A high-performance, concurrent, per-client Java rate-limiting library implementing **Token Bucket** and **Sliding Window Log** algorithms from scratch with zero external runtime dependencies.

---

## 🌟 Key Features

- **Dual Rate-Limiting Algorithms**: Supports both continuous mathematical refill (Token Bucket) and rolling timestamp logs (Sliding Window) under a unified interface.
- **Strict Per-Client Isolation**: State is maintained independently per `clientId` using `ConcurrentHashMap`, preventing high-traffic clients from exhausting other users' quotas.
- **Fine-Grained Concurrency Control**: Uses per-client intrinsic locks to ensure atomic check-and-consume operations without global lock contention across different clients.
- **Zero Third-Party Runtime Dependencies**: Built entirely with the Java Standard Library (`java.util.concurrent`, `java.time`).
- **Interactive Multi-Threaded Simulation**: Built-in CLI simulation comparing bursty vs. steady client patterns with real-time colored logs and metrics.
- **Thoroughly Tested**: 100% test coverage with JUnit 5 covering boundary limits, refill math, isolation, and race-condition resistance.

---

## 📐 Architecture & Project Structure

```text
rate-limiter/
├── src/
│   ├── main/java/ratelimiter/
│   │   ├── RateLimiter.java              # Common interface contract
│   │   ├── TokenBucketRateLimiter.java   # Token Bucket algorithm (O(1) space)
│   │   ├── SlidingWindowRateLimiter.java # Sliding Window Log algorithm (O(M) space)
│   │   ├── RateLimiterFactory.java       # Factory pattern entrypoint
│   │   └── Demo.java                     # Multi-threaded concurrent CLI simulation
│   └── test/java/ratelimiter/
│       ├── TokenBucketRateLimiterTest.java   # JUnit 5 concurrency & boundary tests
│       └── SlidingWindowRateLimiterTest.java # JUnit 5 window slide & isolation tests
├── pom.xml                                   # Maven configuration with Exec & Surefire plugins
├── .gitignore                                # Java / Maven / IDE ignore patterns
└── README.md
```

---

## 🧠 Algorithms & Mechanics

### 1. Token Bucket (`TokenBucketRateLimiter`)
- **How it works**: Each client is assigned a bucket with a maximum token capacity. The bucket continuously accumulates tokens at a fixed refill rate (`tokens/second`). Each incoming request consumes 1 token. If no tokens remain, the request is rejected (`429 Too Many Requests`).
- **Refill Calculation**: Refills lazily upon request arrival using:
  $$\text{tokens} = \min(\text{capacity}, \text{tokens} + \text{elapsedMillis} \times \text{refillRate})$$
- **Strength**: High burst tolerance. An idle client can immediately consume up to their full bucket capacity.
- **Memory**: $O(1)$ space per client.

### 2. Sliding Window Log (`SlidingWindowRateLimiter`)
- **How it works**: Each client maintains a double-ended queue (`Deque<Long>`) of request timestamps. When a request arrives, all timestamps older than `now - windowDuration` are evicted from the front. If the remaining count is strictly less than the allowed limit, the current timestamp is appended and the request is allowed.
- **Strength**: Eliminates the $2\times$ traffic burst vulnerability present at fixed-window boundaries.
- **Memory**: $O(M)$ space per client, where $M$ is the number of requests in the active window.

---

## ⚖️ Algorithm Trade-Off Analysis (Interview Talking Points)

| Dimension | Token Bucket | Sliding Window Log |
| :--- | :--- | :--- |
| **Space Complexity** | **$O(1)$** per client (stores only 2 primitives) | **$O(M)$** per client (stores every timestamp in active window) |
| **Time Complexity** | **$O(1)$** continuous math calculation | **$O(K)$** where $K$ is number of expired timestamps to evict |
| **Burst Tolerance** | **High**: Accumulates tokens up to capacity when idle | **Strict**: Hard ceiling across any sliding duration |
| **Boundary Spike Issue** | None (continuous refill) | None (rolling window prevents boundary spikes) |
| **Best Used For** | General API gateways, microservices (Stripe/AWS style) | High-security endpoints (login, OTP, payment actions) |

---

## 💻 Quickstart & Usage

### 1. Using the Factory Pattern
```java
import ratelimiter.RateLimiter;
import ratelimiter.RateLimiterFactory;
import java.time.Duration;

// 1. Token Bucket: Burst capacity of 10, refills at 2 tokens/sec
RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(10, 2.0);

// 2. Sliding Window Log: Maximum 5 requests per 10-second window
RateLimiter slidingWindow = RateLimiterFactory.createSlidingWindow(5, Duration.ofSeconds(10));

// Check if client request is permitted
if (tokenBucket.allowRequest("user-123")) {
    // Process request (200 OK)
} else {
    // Reject request (429 Too Many Requests)
}
```

---

## 🚀 How to Run in VS Code

### Step 1: Open in VS Code
1. Open **Visual Studio Code**.
2. Click **File** $\rightarrow$ **Open Folder...**
3. Select `C:\Users\Trisha\rate-limiter`.

### Step 2: Run Automated Tests
Open the VS Code Terminal (`Ctrl + \``) and run:
```powershell
.\mvnw.cmd test
```

### Step 3: Run the Concurrent Simulation Demo
Run from the terminal:
```powershell
.\mvnw.cmd compile exec:java
```
Or open [Demo.java](src/main/java/ratelimiter/Demo.java) and click the **Run** button above `public static void main` in VS Code!

---

## 🎯 Resume Bullet

> *"Engineered a thread-safe Java rate-limiting library implementing Token Bucket and Sliding Window Log algorithms with per-client tracking and fine-grained concurrency control; validated correctness via JUnit 5 tests covering boundary conditions, client isolation, and race-condition resistance under high thread contention."*
