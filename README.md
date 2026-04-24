# Password Cracker

## Project Overview

This project implements a **brute-force password cracker** for encrypted ZIP files using Java multi-threading. The program systematically tries every possible password combination until it finds the correct one, demonstrating the performance benefits of concurrent programming.

The cracker works on ZIP files protected with lowercase alphabetic passwords (a-z only) and uses the `zip4j` library to interface with encrypted archives.

---

## Problem Statement

ZIP files can be password-protected, making their contents inaccessible without the correct password. However, there's no limit to how many password attempts you can make. This creates an opportunity for **brute-force attacks** - systematically trying every possible password until finding the right one.

This project explores:
- How to generate all possible password combinations programmatically
- How multi-threading can dramatically speed up brute-force attacks
- How to coordinate concurrent threads using volatile shared state

---

## Project Tasks

### Part 1: Single-Threaded Cracker (3-Character Password)

**Objective:** Crack a ZIP file (`protected3.zip`) protected by a **3-character lowercase password**.

**Requirements:**
- Generate all possible 3-character passwords recursively
- Try each password sequentially until finding the correct one
- Report the password and total time taken
- Uses **1 thread** (single-threaded execution)

**Search Space:** 26³ = **17,576 possible passwords**

---

### Part 2: Multi-Threaded Cracker (5-Character Password)

**Objective:** Crack a ZIP file (`protected5.zip`) protected by a **5-character lowercase password** using multiple threads.

**Requirements:**
- Divide the search space into non-overlapping ranges
- Spawn multiple worker threads (configurable via `numThreads` variable)
- Each thread works independently on its assigned range
- All threads terminate immediately when any thread finds the password
- Each thread operates on its own copy of the ZIP file to avoid bottlenecks
- Report timing for both **3 threads** and **4 threads**

**Search Space:** 26⁵ = **11,881,376 possible passwords**

**Key Challenge:** The larger search space makes single-threaded execution impractically slow, demonstrating the real-world need for parallelization.

---

## Architecture

The project uses **4 Java classes** working together:

### 1. **`Main.java`** - Driver Class

**Purpose:** Application entry point and timing coordinator.

**Responsibilities:**
- Defines paths to the encrypted ZIP files
- Runs Part 1 (single-threaded) by calling `ThreadCracker.crack()` with `numThreads = 1`
- Runs Part 2 (multi-threaded) by calling `ThreadCracker.crack()` with `numThreads = 3` or `4`
- Contains a comment block at the top to record timing results

**Key Features:**
- Centralized configuration for file paths
- Easy switching between 3 and 4 threads for performance comparison
- Clean separation between single-threaded and multi-threaded execution

---

### 2. **`ThreadCracker.java`** - Unified Cracker Orchestrator

**Purpose:** Manages the entire password-cracking workflow for both single-threaded and multi-threaded modes.

**Responsibilities:**
- Computes the total search space size (26^passwordLength)
- Divides the search space into equal chunks for each thread
- Creates and starts `CrackerThread` worker instances
- Waits for all threads to complete using `Thread.join()`
- Reports detailed timing breakdown (setup time, search time, total time)

**How Work Distribution Works:**
```
Total passwords: 11,881,376 (for 5-character password)
With 4 threads:
  Thread 0: indices [0,           2,970,344)
  Thread 1: indices [2,970,344,   5,940,688)
  Thread 2: indices [5,940,688,   8,911,032)
  Thread 3: indices [8,911,032,  11,881,376)  ← absorbs remainder
```

**Key Design Decision:**
When `numThreads = 1`, the single thread gets the entire range `[0, 11881376)` - making it functionally single-threaded **without any special-case code**.

---

### 3. **`CrackerThread.java`** - Worker Thread

**Purpose:** Individual worker that searches a specific range of password indices.

**Responsibilities:**
- Receives an assigned index range `[startIndex, endIndex)`
- Creates a private copy of the ZIP file to avoid file-access conflicts
- Converts each numeric index to a password string using `PasswordUtils.indexToPassword()`
- Tests each password against the ZIP file using `zip4j`
- Sets the shared `volatile` flags when the password is found
- Cleans up private files (ZIP copy and extraction directory) before terminating

**Shared Volatile State:**
```java
public static volatile boolean passwordFound = false;  // Signal to all threads
public static volatile String foundPassword = null;    // Stores the result
```

**Why Volatile?**
The `volatile` keyword ensures that when one thread writes to these variables, all other threads immediately see the updated value (bypassing CPU cache). This allows instant termination of all threads once the password is discovered.

**File Isolation:**
Each thread works on:
- **Private ZIP copy:** `protected-copy-{threadId}.zip`
- **Private extraction directory:** `contents-{threadId}/`

This prevents race conditions and I/O bottlenecks that would occur if all threads tried to access the same file simultaneously.

---

### 4. **`PasswordUtils.java`** - Password Generation Utilities

**Purpose:** Provides mathematical helpers for password enumeration.

**Key Methods:**

#### `totalPasswords(int length)`
Calculates the total number of possible passwords:
```java
26^length = total combinations
```

#### `indexToPassword(long index, int length)`
Converts a numeric index to its corresponding password string using **base-26 positional notation**:

```
Index   → Password (length=3)
-----     --------
0       → "aaa"
1       → "aab"
25      → "aaz"
26      → "aba"
...
17575   → "zzz"
```

**Why Index-Based Generation?**
Instead of generating passwords recursively (which is hard to parallelize), we treat the entire search space as a numbered list. Each thread simply iterates through its assigned numeric range and converts indices to passwords on-the-fly.

**Benefits:**
- ✅ Perfect work distribution (no overlap between threads)
- ✅ No synchronization needed (each thread owns a disjoint range)
- ✅ Trivial to split work: divide `[0, total)` into N equal chunks

---

## Compilation & Execution

### Prerequisites
- Java 8 or higher
- `zip4j-1.3.2.jar` (place in `lib/` directory)

### Compile
```bash
javac -cp lib/zip4j-1.3.2.jar -d bin src/passwordcracker/*.java
```

### Run
**Windows:**
```bash
java -cp "src;lib/zip4j-1.3.2.jar" passwordcracker.Main
```

**Mac/Linux:**
```bash
java -cp "src:lib/zip4j-1.3.2.jar" passwordcracker.Main
```

---

Performance Results
-------------------

### Part 1: Single-Threaded (3-character password)

| Metric | Time (ms) |
| --- | --- |
| Search | 1,136 |

* * * * *

### Part 2: Multi-Threaded (5-character password)

| Threads | Search Time (ms) | Speedup vs 3 Threads |
| --- | --- | --- |
| 3 | 982,780 | 1.00× (baseline) |
| 4 | 178,723 | ~5.50× |

**Instructions:**
1. Run the program with `numThreads = 3` and record the times
2. Change `numThreads = 4` in `Main.java` and run again
3. Fill in the table above
4. Calculate speedup: `Time(3 threads) / Time(4 threads)`

---

## Key Takeaways

### Concurrency Benefits Demonstrated
- Multi-threading provides **near-linear speedup** for CPU-bound tasks
- 4 threads can theoretically search 4× faster than 1 thread (actual speedup depends on overhead)
- For the 5-character password (11.8M candidates), multi-threading is **essential** for reasonable execution time

### Java Concurrency Concepts Applied
- **Thread creation and lifecycle** (`extends Thread`, `start()`, `join()`)
- **Volatile variables** for inter-thread communication without locks
- **Work partitioning** to avoid synchronization overhead
- **Resource cleanup** (each thread manages its own files)

### Real-World Security Implications
- This project demonstrates why **strong passwords matter**
- 3-character password: crackable in seconds
- 5-character password: minutes with multi-threading
- 8+ character passwords with mixed case/symbols: **computationally infeasible** with brute-force

---

## Project Structure

``` bash
├── lib/                                   # External libraries (JAR files)
├── res/                                   # Assignment requirements documentation
│   ├── Help with password generation.pdf
│   └── PasswordCracker.pdf
├── src/                                   # Java source code
│   └── passwordcracker/                   # Package directory
│       ├── CrackerThread.java             # Worker thread that tests passwords
│       ├── Example.java                   # Demo/example usage (if any)
│       ├── Main.java                      # Entry point, runs with 1/3/4 threads
│       ├── Orchestrator.java              # Splits work, spawns and manages threads
│       └── PasswordUtils.java             # Index to password conversion utilities
├── .gitignore                             # Git ignored files (build, temp, IDE files)
└── README.md                              # Project documentation
```

---
