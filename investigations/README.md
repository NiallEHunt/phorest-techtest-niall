# Investigations

> **This module is an investigation, not part of the product.** It's kept in the repository for historical reasons, as evidence for a decision in the decision log. In a real project it would not be committed. It isn't used by the game or the service, and `./gradlew test` doesn't run it.
>
> I leaned more heavily on AI for this investigation than for the rest of the project. See the AI usage log.

## Colour representation: `String` value class vs `Int`

### The question

In the core, a colour is a `String` value class (`Colour(val name: String)`), and a spin is a `List<Colour>`. The obvious alternative is to represent each colour by its index in the config's colour list, as an `Int`. That's usually assumed to be faster. This benchmark measures how much faster, so the decision is based on numbers.

### What is compared

The investigation ran in two rounds. The first compared the `Colour` list with `Int` representations. The second added the improved algorithm the first round pointed to, two more representations, more colours, a whole-play benchmark and allocation measurements. The third played lots of small machines at once on many threads (see [Many machines at once](#many-machines-at-once)).

Representations:

| Representation | Spin type | Notes |
|---|---|---|
| **Colour list** (what the core uses) | `List<Colour>` | A value class is boxed when stored in a `List`, so each slot gets its own small wrapper object |
| **Plain colour list** (round 2) | `List<PlainColour>` | A plain `data class PlainColour(val name: String)`. Each slot points at a shared instance from the config's colour list, so a spin creates no object per slot |
| **Int list** | `List<Int>` | A colour is its index in the config's colour list |
| **Int array** | `IntArray` | The same index, stored as a primitive |
| **Spin** (round 2) | `Spin` wrapping an `IntArray` | Primitive storage, but read through an `Int` value class (`ColourIndex`), so callers keep a distinct colour type |

Evaluation algorithms:

| Algorithm | Used with | How it works |
|---|---|---|
| **Core** | Colour list, Int list, Int array | The core's `computeOutcomeOf` (or an `Int` copy of it): `distinct()` twice over the whole row, then the run check |
| **Primitive** | Int array | Counts distinct colours in a `BooleanArray` over the whole row, so nothing is boxed or hashed |
| **Single pass** (round 2) | Colour list, plain colour list, Spin | Checks all three rules in one pass and stops once the outcome is certain. The list versions use a `HashSet`, which they stop filling at the first repeated colour; the Spin version uses a `BooleanArray` sized to the colour list |

Every evaluator is checked against the core before anything is timed, on the benchmark's own spin and on 10,000 small random spins that reach all four outcomes.

Operations:
- **spin:** filling `slotCount` slots with random colours;
- **evaluate:** working out the outcome of a spin (jackpot, full house, small prize with `k = 2`);
- **play** (round 2): spin, evaluate, then turn the slots into the list of colour names a response needs. The index-based representations pay for looking their names back up here; the spin and evaluate benchmarks don't charge them for it.

Parameters:
- **`slotCount`:** 4 (the original game), 100, 10,000 and 1,000,000 slots. We didn't go higher: at 1,000,000 slots a response would already be around 10 MB of colour names, so beyond that the response, not the representation, would dominate.
- **`colourCount`:** 4 (the original game), 300 and 100,000. 300 is above 127, the largest `Integer` the JVM caches, so most `List<Int>` slots need a newly allocated `Integer`. With 100,000 colours a large row has mostly distinct colours, so the distinct-colour set keeps growing instead of stopping at a few hundred.

### How to run it

```sh
./gradlew :investigations:benchmark
```

It uses [kotlinx-benchmark](https://github.com/Kotlin/kotlinx-benchmark), JetBrains' benchmarking toolkit, which runs [JMH](https://github.com/openjdk/jmh) on the JVM. Each benchmark has 3 warm-up iterations, then 5 measured iterations of 1 second, in a forked JVM. Results are written to `investigations/build/reports/benchmarks/`.

kotlinx-benchmark has no option for JMH's profilers, so round 2 ran the JMH jar the plugin builds directly, with the same settings plus JMH's GC profiler (`-prof gc`). The profiler reports the bytes allocated per operation alongside each time:

```sh
./gradlew :investigations:mainBenchmarkJar
java -jar investigations/build/benchmarks/main/jars/investigations-main-jmh-JMH.jar \
  ColourRepresentationBenchmark -wi 3 -w 1s -i 5 -r 1s -f 1 -bm avgt -tu ns -prof gc \
  -rf json -rff investigations/build/reports/benchmarks/gc/results.json
```

A full run takes about 30 minutes.

The many-machines benchmark (round 3) runs in throughput mode, and JMH's `-t` option sets how many threads play at once. kotlinx-benchmark can't set either per benchmark, so it's also run from the JMH jar, once per thread count (about 3 minutes each):

```sh
java -jar investigations/build/benchmarks/main/jars/investigations-main-jmh-JMH.jar \
  ManyMachinesBenchmark -wi 3 -w 1s -i 5 -r 1s -f 1 -t 6 -bm thrpt -tu us -prof gc \
  -rf json -rff investigations/build/reports/benchmarks/many-machines/threads-6.json
```

### Results

Run on 2026-10-05 (round 2, which re-ran every round 1 benchmark): JDK 25.0.4, WSL2 on a desktop PC, 1 fork, average time per operation, with JMH's GC profiler. Each cell shows the time, how many times faster than the first column it was (in brackets), and the bytes allocated per operation. Lower is better. Raw output is in `build/reports/benchmarks/gc/` after a run.

The round 1 figures fell within the noise of these. The error margins are wide (often ±10–20%, and up to ±250% for the noisiest runs, mostly 1,000,000-slot runs and runs allocating about 100 KB per operation, where garbage collection pauses land in some iterations but not others). Treat time differences under about 1.5× as noise. The allocation figures are much more stable than the times.

#### Spin

4 colours:

| Slots | Colour list | Plain colour list | Int list | Int array |
|---|---|---|---|---|
| 4 | 37 ns<br>120 B | 31 ns (1.2×)<br>56 B | 28 ns (1.3×)<br>56 B | 11 ns (3.2×)<br>32 B |
| 100 | 764 ns<br>2.0 KB | 681 ns (1.1×)<br>440 B | 617 ns (1.2×)<br>440 B | 131 ns (5.8×)<br>416 B |
| 10,000 | 82.2 µs<br>195.4 KB | 70.6 µs (1.2×)<br>39.1 KB | 57.9 µs (1.4×)<br>39.1 KB | 34.2 µs (2.4×)<br>39.1 KB |
| 1,000,000 | 17.4 ms<br>19.1 MB | 11.9 ms (1.5×)<br>3.8 MB | 12.6 ms (1.4×)<br>3.8 MB | 6.7 ms (2.6×)<br>3.8 MB |

300 colours:

| Slots | Colour list | Plain colour list | Int list | Int array |
|---|---|---|---|---|
| 4 | 39 ns<br>120 B | 37 ns (1.1×)<br>56 B | 56 ns (0.7×)<br>93 B | 25 ns (1.6×)<br>32 B |
| 100 | 892 ns<br>2.0 KB | 825 ns (1.1×)<br>440 B | 1.3 µs (0.7×)<br>1.3 KB | 615 ns (1.5×)<br>416 B |
| 10,000 | 86.1 µs<br>195.4 KB | 81.6 µs (1.1×)<br>39.1 KB | 127.5 µs (0.7×)<br>128.7 KB | 61.5 µs (1.4×)<br>39.1 KB |
| 1,000,000 | 18.2 ms<br>19.1 MB | 14.3 ms (1.3×)<br>3.8 MB | 21.9 ms (0.8×)<br>12.6 MB | 10.3 ms (1.8×)<br>3.8 MB |

100,000 colours:

| Slots | Colour list | Plain colour list | Int list | Int array |
|---|---|---|---|---|
| 4 | 70 ns<br>120 B | 66 ns (1.1×)<br>56 B | 31 ns (2.3×)<br>120 B | 21 ns (3.3×)<br>32 B |
| 100 | 1.6 µs<br>2.0 KB | 1.5 µs (1.1×)<br>440 B | 705 ns (2.3×)<br>2.0 KB | 487 ns (3.4×)<br>416 B |
| 10,000 | 150.2 µs<br>195.4 KB | 144.3 µs (1.0×)<br>39.1 KB | 66.1 µs (2.3×)<br>195.2 KB | 52.4 µs (2.9×)<br>39.1 KB |
| 1,000,000 | 28.6 ms<br>19.1 MB | 20.3 ms (1.4×)<br>3.8 MB | 16.3 ms (1.8×)<br>19.1 MB | 9.3 ms (3.1×)<br>3.8 MB |

#### Evaluate, core algorithm

4 colours:

| Slots | Colour list | Int list | Int array | Int array, primitive |
|---|---|---|---|---|
| 4 | 178 ns<br>704 B | 153 ns (1.2×)<br>736 B | 134 ns (1.3×)<br>672 B | 9 ns (20.5×)<br>24 B |
| 100 | 3.2 µs<br>2.6 KB | 2.0 µs (1.6×)<br>2.6 KB | 1.9 µs (1.7×)<br>2.6 KB | 53 ns (61.1×)<br>24 B |
| 10,000 | 317.0 µs<br>128.7 KB | 249.0 µs (1.3×)<br>128.7 KB | 238.4 µs (1.3×)<br>128.7 KB | 5.3 µs (60.0×)<br>24 B |
| 1,000,000 | 48.4 ms<br>16.0 MB | 43.3 ms (1.1×)<br>16.0 MB | 42.3 ms (1.1×)<br>16.0 MB | 366.7 µs (132×)<br>27 B |

300 colours:

| Slots | Colour list | Int list | Int array | Int array, primitive |
|---|---|---|---|---|
| 4 | 181 ns<br>816 B | 153 ns (1.2×)<br>816 B | 167 ns (1.1×)<br>816 B | 19 ns (9.4×)<br>320 B |
| 100 | 3.6 µs<br>10.4 KB | 3.0 µs (1.2×)<br>10.5 KB | 2.8 µs (1.3×)<br>12.3 KB | 105 ns (34.4×)<br>320 B |
| 10,000 | 314.2 µs<br>156.4 KB | 318.2 µs (1.0×)<br>156.4 KB | 282.2 µs (1.1×)<br>336.4 KB | 4.8 µs (65.5×)<br>320 B |
| 1,000,000 | 50.7 ms<br>16.0 MB | 51.0 ms (1.0×)<br>16.0 MB | 38.3 ms (1.3×)<br>33.5 MB | 370.8 µs (137×)<br>323 B |

100,000 colours:

| Slots | Colour list | Int list | Int array | Int array, primitive |
|---|---|---|---|---|
| 4 | 173 ns<br>816 B | 149 ns (1.2×)<br>816 B | 140 ns (1.2×)<br>848 B | 6.8 µs (0.03×)<br>97.7 KB |
| 100 | 3.5 µs<br>11.7 KB | 3.1 µs (1.1×)<br>11.7 KB | 3.1 µs (1.1×)<br>14.8 KB | 6.9 µs (0.5×)<br>97.7 KB |
| 10,000 | 760.0 µs<br>1021.1 KB | 456.1 µs (1.7×)<br>1021.1 KB | 443.3 µs (1.7×)<br>1.3 MB | 23.3 µs (32.6×)<br>97.7 KB |
| 1,000,000 | 194.2 ms<br>25.2 MB | 81.1 ms (2.4×)<br>25.2 MB | 86.0 ms (2.3×)<br>55.6 MB | 1.2 ms (168×)<br>97.7 KB |

#### Evaluate, single pass

4 colours:

| Slots | Colour list, core (baseline) | Colour list | Plain colour list | Spin |
|---|---|---|---|---|
| 4 | 178 ns<br>704 B | 79 ns (2.2×)<br>224 B | 76 ns (2.3×)<br>240 B | 10 ns (17.2×)<br>24 B |
| 100 | 3.2 µs<br>2.6 KB | 86 ns (37.3×)<br>240 B | 97 ns (33.2×)<br>240 B | 10 ns (312×)<br>24 B |
| 10,000 | 317.0 µs<br>128.7 KB | 83 ns (3,803×)<br>240 B | 88 ns (3,583×)<br>240 B | 10 ns (32,548×)<br>24 B |
| 1,000,000 | 48.4 ms<br>16.0 MB | 84 ns (578,755×)<br>240 B | 92 ns (523,960×)<br>240 B | 10 ns (4,907,327×)<br>24 B |

300 colours:

| Slots | Colour list, core (baseline) | Colour list | Plain colour list | Spin |
|---|---|---|---|---|
| 4 | 181 ns<br>816 B | 90 ns (2.0×)<br>256 B | 74 ns (2.4×)<br>272 B | 22 ns (8.2×)<br>320 B |
| 100 | 3.6 µs<br>10.4 KB | 750 ns (4.8×)<br>1.4 KB | 763 ns (4.7×)<br>1.5 KB | 77 ns (46.8×)<br>320 B |
| 10,000 | 314.2 µs<br>156.4 KB | 856 ns (367×)<br>1.5 KB | 753 ns (417×)<br>1.5 KB | 78 ns (4,009×)<br>320 B |
| 1,000,000 | 50.7 ms<br>16.0 MB | 745 ns (68,022×)<br>1.5 KB | 822 ns (61,662×)<br>1.4 KB | 78 ns (646,560×)<br>320 B |

100,000 colours:

| Slots | Colour list, core (baseline) | Colour list | Plain colour list | Spin |
|---|---|---|---|---|
| 4 | 173 ns<br>816 B | 77 ns (2.3×)<br>272 B | 80 ns (2.2×)<br>256 B | 7.0 µs (0.02×)<br>97.7 KB |
| 100 | 3.5 µs<br>11.7 KB | 2.8 µs (1.2×)<br>5.2 KB | 3.1 µs (1.2×)<br>5.2 KB | 7.2 µs (0.5×)<br>97.7 KB |
| 10,000 | 760.0 µs<br>1021.1 KB | 130.3 µs (5.8×)<br>15.6 KB | 139.3 µs (5.5×)<br>15.6 KB | 20.4 µs (37.3×)<br>97.7 KB |
| 1,000,000 | 194.2 ms<br>25.2 MB | 344.0 µs (565×)<br>15.6 KB | 442.8 µs (438×)<br>15.6 KB | 35.6 µs (5,457×)<br>97.7 KB |

#### Play

4 colours:

| Slots | Colour list, core | Colour list, single pass | Plain colour list, single pass | Spin, single pass |
|---|---|---|---|---|
| 4 | 285 ns<br>868 B | 143 ns (2.0×)<br>415 B | 123 ns (2.3×)<br>335 B | 54 ns (5.3×)<br>136 B |
| 100 | 4.6 µs<br>5.1 KB | 1.2 µs (3.7×)<br>2.6 KB | 1.2 µs (3.9×)<br>1.1 KB | 518 ns (9.0×)<br>904 B |
| 10,000 | 427.2 µs<br>363.2 KB | 131.0 µs (3.3×)<br>234.7 KB | 116.7 µs (3.7×)<br>78.4 KB | 75.4 µs (5.7×)<br>78.2 KB |
| 1,000,000 | 56.2 ms<br>38.9 MB | 26.9 ms (2.1×)<br>22.9 MB | 21.8 ms (2.6×)<br>7.6 MB | 17.2 ms (3.3×)<br>7.6 MB |

300 colours:

| Slots | Colour list, core | Colour list, single pass | Plain colour list, single pass | Spin, single pass |
|---|---|---|---|---|
| 4 | 356 ns<br>1014 B | 159 ns (2.2×)<br>455 B | 164 ns (2.2×)<br>407 B | 87 ns (4.1×)<br>432 B |
| 100 | 8.7 µs<br>12.7 KB | 3.1 µs (2.8×)<br>3.4 KB | 3.1 µs (2.8×)<br>1.9 KB | 1.2 µs (7.3×)<br>1.2 KB |
| 10,000 | 524.8 µs<br>390.9 KB | 135.6 µs (3.9×)<br>235.5 KB | 127.8 µs (4.1×)<br>79.2 KB | 103.9 µs (5.1×)<br>78.5 KB |
| 1,000,000 | 66.4 ms<br>38.9 MB | 29.8 ms (2.2×)<br>22.9 MB | 28.2 ms (2.4×)<br>7.6 MB | 22.0 ms (3.0×)<br>7.6 MB |

100,000 colours:

| Slots | Colour list, core | Colour list, single pass | Plain colour list, single pass | Spin, single pass |
|---|---|---|---|---|
| 4 | 430 ns<br>1016 B | 298 ns (1.4×)<br>472 B | 320 ns (1.3×)<br>408 B | 13.4 µs (0.03×)<br>97.8 KB |
| 100 | 8.8 µs<br>14.1 KB | 9.7 µs (0.9×)<br>7.5 KB | 9.8 µs (0.9×)<br>6.0 KB | 12.2 µs (0.7×)<br>98.5 KB |
| 10,000 | 1.5 ms<br>1.2 MB | 458.8 µs (3.2×)<br>252.8 KB | 494.3 µs (3.0×)<br>96.5 KB | 247.2 µs (6.0×)<br>175.9 KB |
| 1,000,000 | 335.2 ms<br>48.0 MB | 38.9 ms (8.6×)<br>22.9 MB | 38.0 ms (8.8×)<br>7.6 MB | 32.1 ms (10.4×)<br>7.7 MB |

### Findings

1. **The algorithm matters far more than the type, as round 1 suggested.** The single-pass algorithm on the same `List<Colour>` evaluates the original 4-slot game 2.2× faster with a third of the allocation, and a whole play is twice as fast. On large rows, the outcome of a random spin is usually certain after a few slots (two adjacent slots match), so evaluation takes constant time: about 80 ns at 1,000,000 slots, against 48 ms for the core. Even with 100,000 colours, where matches are rare and the row has to be scanned much further, it's faster at every size: 1.2× at 100 slots, rising to 560× at 1,000,000.
2. **Swapping in ints alone still gains little.** With the core's algorithm, `Int` lists and arrays evaluate about 1–2.4× faster than the `Colour` list, and they allocate about as much or more. `List<Int>` with 300 colours allocates more than three times as much as `IntArray` when spinning (12.6 MB against 3.8 MB at 1,000,000 slots), which confirms that indices above 127 each need a new `Integer`.
3. **The value class is boxed in every slot.** A spin into `List<Colour>` allocates about 20 bytes per slot (19.1 MB at 1,000,000 slots), against about 4 for a plain class (3.8 MB, just the list's array of references): the value class creates a new `Colour` wrapper for every slot, while the plain class reuses the instances in the config's colour list. Over a whole play, the plain class allocates up to 3× less from 100 slots up (less of a saving with 100,000 colours, where the hash set dominates), and 10–20% less at 4 slots, though the time difference is mostly within the noise. The value class gives no benefit here, because a colour almost always lives in a `List`.
4. **The primitive approach costs more as the number of colours grows.** Counting colours in a `BooleanArray` is the fastest option with few colours, but the array is as long as the colour list. With 100,000 colours, a 4-slot play allocates about 98 KB and takes 13 µs, against 0.3 µs for the `Colour` list. That's 40 times slower on exactly the small machines the service will mostly run. Fixing it means more machinery (an int hash set, or reusing the array per thread), on top of a table that maps names to indices and back.
5. **More slots wouldn't change the picture.** At 1,000,000 slots every variant is already dominated by building and returning the row itself, and a response would be around 10 MB of names. Raising the colour count to 100,000 was more revealing: it showed the core algorithm's real worst case (194 ms to evaluate 1,000,000 slots) and the primitive approach's weakness (finding 4).
6. **Small machines are cheap whatever the representation.** A 4-slot play takes 50–430 ns and allocates under 1 KB in every variant, apart from the primitive one with 100,000 colours. Even at 10,000 plays a second that's around 10 MB/s of short-lived garbage, which the JVM's garbage collector handles easily. For lots of small machines, the HTTP request, JSON and per-machine locking will cost far more than the play itself.

### Many machines at once

The benchmarks above play one machine on one thread, so its colours stay in the CPU's caches. A service will mostly run lots of small machines, played at once on many threads. `ManyMachinesBenchmark` covers that:

- Each machine has 4 slots and its own colour list, with its own `String` instances (as if each config had been deserialised from JSON separately).
- Every play picks a random machine, then spins, evaluates and builds the colour names a response needs, as in the play benchmarks above.
- Each thread has its own random generator.
- It compares 1 machine (the cache-friendly baseline) with 10,000 machines, on 1, 6 and 12 threads.
- Machines are only read, never updated: the float and the per-machine locking belong to the service, which doesn't exist yet.

Run on the same day and desktop PC. Throughput is added up across all threads. Error margins were at most ±23%, and the bytes allocated per play were the same at every thread count.

4 colours, millions of plays per second (higher is better):

| Variant | 1 thread, 1 machine | 1 thread, 10,000 machines | 6 threads, 1 machine | 6 threads, 10,000 machines | 12 threads, 1 machine | 12 threads, 10,000 machines | Allocated per play |
|---|---|---|---|---|---|---|---|
| Colour list, core | 3.6 | 3.3 | 17.4 | 16.6 | 19.6 | 21.6 | 868 B |
| Colour list, single pass | 6.8 | 5.8 | 36.5 | 30.5 | 34.3 | 39.2 | 399 B |
| Plain colour list, single pass | 7.2 | 6.0 | 24.1 | 32.0 | 27.0 | 46.0 | 335 B |
| Spin, single pass | 17.9 | 13.5 | 86.8 | 63.1 | 53.9 | 91.1 | 136 B |

300 colours, millions of plays per second (higher is better):

| Variant | 1 thread, 1 machine | 1 thread, 10,000 machines | 6 threads, 1 machine | 6 threads, 10,000 machines | 12 threads, 1 machine | 12 threads, 10,000 machines | Allocated per play |
|---|---|---|---|---|---|---|---|
| Colour list, core | 3.9 | 1.0 | 18.1 | 4.8 | 19.2 | 7.1 | 934 B |
| Colour list, single pass | 6.1 | 1.1 | 28.2 | 5.7 | 31.7 | 8.4 | 455 B |
| Plain colour list, single pass | 6.2 | 1.1 | 27.7 | 5.5 | 40.4 | 8.3 | 391 B |
| Spin, single pass | 14.2 | 2.6 | 46.5 | 12.0 | 44.8 | 17.2 | 432 B |

Findings:

1. **When machines don't fit in the CPU's cache, memory access dominates.** 10,000 machines with 4 colours each (about 40,000 colours) fit in the 16 MB cache, and throughput barely drops from the 1-machine baseline. 10,000 machines with 300 colours each (about 3 million colours, hundreds of MB) don't: every variant drops 3–6×. The core and single-pass `Colour` versions then run at almost the same speed (1.0 against 1.1 million plays a second on one thread), because waiting for memory costs more than the algorithm.
2. **The `Spin` looks about 2.4× faster with many colours, but that's mostly an artefact.** It only copies references to the colour names and never reads the `String`s themselves. The `Colour` variants read each `String` to compare and hash it, so each slot is another trip to memory. A real response would read every name to serialise it to JSON, so the `Spin` would pay that cost too.
3. **Plays scale with cores, and allocation doesn't hold them back.** Going from 1 to 6 threads gives about 3–5× the throughput in every variant. 12 threads add little beyond that, or even lose some, because the 12 threads share 6 physical cores. At the peak, the core version was allocating about 19 GB/s of short-lived garbage (21.6 million plays a second × 868 bytes) and still scaling. The JVM's garbage collector isn't the bottleneck.
4. **The worst case is still about 1 µs a play.** The slowest result (the core `Colour` list, 300 colours, 10,000 machines, one thread) is about 1 million plays a second. A single HTTP request costs tens of microseconds or more, so with lots of small machines the service's own work (HTTP, JSON, locking) will dominate whichever representation is used.

### Limits of this investigation

- Each evaluate benchmark times one fixed random spin. A random spin rarely hits the single-pass algorithm's worst case: a row with no early answer, such as a large full house with every colour different. With the 100,000-colour rows, which scan far before finding a match, it was still 1.2–560× faster than the core, but a deliberate worst-case row wasn't measured.
- The many-machines benchmark doesn't include the service's own work (HTTP, JSON, updating the float under a per-machine lock), which will likely cost more than the play itself. That should be measured with a load test once the service exists.

### Conclusion

Keep colours as names (`String`), not indices. Ints win only in narrow cases, they lose badly with many colours on small machines, and they need a table to translate between numbers and names at the API. With lots of small machines on many threads, memory access and (in the real service) HTTP dominate, so the representation matters even less. A rough AWS capacity and cost estimate (`aws_cost_estimate.ai-generated.md`, in this folder) puts the game logic at about 0.2% of the CPU a play costs once the service's own work is included.

Two changes are worth considering in the core:

1. **Evaluate in a single pass that stops early.** This is the change that matters, in line with the brief's request for a solution that stays efficient as the number of slots grows. It's faster at every size measured except one: with 100,000 colours and 100 slots, a whole play was within the noise.
2. **Possibly make `Colour` a plain class instead of a value class.** It allocates less in every spin, at no measurable cost. This is a smaller, optional gain: with the single-pass algorithm in place, the value class costs about 50–80 bytes per 4-slot play.
