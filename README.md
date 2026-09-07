# Thread-Safe Java Rate Limiter Library

A high-performance, concurrent, per-client Java rate-limiting library implementing **Token Bucket** and **Sliding Window Log** algorithms.

---

## 🌟 Key Features

- **Dual Strategy Implementation**: Includes both Token Bucket (continuous refill) and Sliding Window Log (rolling timestamp deque) under a unified `RateLimiter` interface.
- **Strict Per-Client Isolation**: State is tracked per `clientId` using `ConcurrentHashMap`, ensuring traffic spikes on one client do not impact others.
- **Thread-Safe Architecture**: Employs fine-grained per-client intrinsic locking to guarantee atomic read-modify-write operations without global lock contention.
- **Zero External Dependencies**: Core engine built with pure Java Standard Library; uses JUnit 5 for testing.
- **Interactive Multi-Threaded Simulation**: Built-in CLI runner demonstrating burst vs. steady traffic patterns with colored real-time logs.

---

## 📐 Architecture & Project Structure

```text
rate-limiter/
├── src/
│   ├── main/java/ratelimiter/
│   │   ├── RateLimiter.java              # Common interface contract
│   │   ├── TokenBucketRateLimiter.java   # Token Bucket algorithm (O(1) memory)
│   │   ├── SlidingWindowRateLimiter.java # Sliding Window Log algorithm (O(M) memory)
│   │   ├── RateLimiterFactory.java       # Factory pattern entrypoint
│   │   └── Demo.java                     # Multi-threaded concurrent CLI simulation
│   └── test/java/ratelimiter/
│       ├── TokenBucketRateLimiterTest.java
│       └── SlidingWindowRateLimiterTest.java
├── pom.xml
└── README.md
```

---

## ⚖️ Algorithm Trade-Off Analysis (Interview Cheat Sheet)

| Dimension | Token Bucket | Sliding Window Log |
| :--- | :--- | :--- |
| **Space Complexity** | **$O(1)$** per client (stores only 2 numbers: tokens & timestamp) | **$O(M)$** per client (stores every timestamp in active window) |
| **Time Complexity** | **$O(1)$** math operations | **$O(K)$** where $K$ is number of expired timestamps to evict |
| **Burst Tolerance** | **High**: Accumulates tokens up to capacity during idle periods | **Strict**: Enforces an absolute ceiling in any rolling window |
| **Boundary Spike Issue** | None (continuous refill) | None (sliding window eliminates 2x boundary spike) |
| **Best Used For** | General API gateways, microservices, AWS/Stripe-style rate limits | High-security endpoints (login, OTP, payment actions) |

---

## 🚀 How to Run in VS Code

### Step 1: Open in VS Code
1. Open **Visual Studio Code**.
2. Click **File** $\rightarrow$ **Open Folder...**
3. Select `C:\Users\Trisha\rate-limiter`.
4. If prompted, install the recommended **Extension Pack for Java**.

### Step 2: Run Automated Tests
Open the VS Code Terminal (`Ctrl + \``) and run:
```powershell
.\mvnw.cmd test
```

### Step 3: Run the Multi-Threaded Simulation Demo
Run from the terminal:
```powershell
.\mvnw.cmd compile exec:java
```
Or directly run `Demo.java` by clicking the **Run** icon above `public static void main` in VS Code!

---

## 🎯 Resume Bullet

> *"Engineered a thread-safe Java rate-limiting library implementing Token Bucket and Sliding Window Log algorithms with per-client tracking and fine-grained concurrency control; validated correctness via JUnit 5 tests covering boundary conditions, client isolation, and race-condition resistance under high thread contention."*
