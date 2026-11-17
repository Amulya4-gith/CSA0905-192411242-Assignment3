import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

class ProcessingException extends Exception {
    ProcessingException(String message) { super(message); }
    ProcessingException(String message, Throwable cause) { super(message, cause); }
}

record Result(String file, String student, int total, String grade) { }
record Failure(String file, String reason) { }

class Scoring {
    /** reads one score file; every problem is reported as a ProcessingException with a clear reason */
    static Result process(Path p, int delayMicros) throws ProcessingException {
        if (delayMicros > 0) { try { Thread.sleep(delayMicros / 1000, (delayMicros % 1000) * 1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
        try {
            List<String> lines = Files.readAllLines(p);
            if (lines.isEmpty()) throw new ProcessingException("empty file");
            String student = lines.get(0).trim();
            if (!student.matches("STU\\d{5}")) throw new ProcessingException("bad student id");
            int total = 0, n = 0;
            for (int i = 1; i < lines.size(); i++) {
                int mark = Integer.parseInt(lines.get(i).split(",")[1].trim());
                if (mark < 0 || mark > 100) throw new ProcessingException("mark out of range");
                total += mark; n++;
            }
            if (n != 5) throw new ProcessingException("wrong number of subjects");
            int avg = total / 5;
            String grade = avg >= 75 ? "A" : avg >= 60 ? "B" : avg >= 40 ? "C" : "F";
            return new Result(p.getFileName().toString(), student, total, grade);
        } catch (NoSuchFileException e) {
            throw new ProcessingException("file missing", e);
        } catch (IOException e) {
            throw new ProcessingException("unreadable file", e);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new ProcessingException("malformed line", e);
        }
    }
}

class Summary {
    final ConcurrentHashMap<String, Result> ok = new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<Failure> bad = new ConcurrentLinkedQueue<>();
    final LongAdder lost = new LongAdder();      // tasks whose Future itself failed (an Error, for example)
    Map<String, Long> reasons() { return bad.stream().collect(Collectors.groupingBy(Failure::reason, TreeMap::new, Collectors.counting())); }
    long gradeTotal() { return ok.values().stream().mapToLong(Result::total).sum(); }
}

public class Processor {
    static List<Path> files = new ArrayList<>();
    static Map<String, Long> expectedReasons = new TreeMap<>();
    static long expectedOk, expectedTotals;

    static void generate(Path dir, int n) throws IOException {
        Files.createDirectories(dir);
        expectedReasons.clear(); expectedOk = 0; expectedTotals = 0; files.clear();
        for (int i = 1; i <= n; i++) {
            Path p = dir.resolve(String.format("score%05d.txt", i));
            files.add(p);
            if (i % 50 == 7) { Files.deleteIfExists(p); expectedReasons.merge("file missing", 1L, Long::sum); continue; }
            String id = String.format("STU%05d", i);
            int[] m = new int[5]; int total = 0;
            for (int k = 0; k < 5; k++) { m[k] = (i * 7 + k * 13) % 101; total += m[k]; }
            List<String> lines = new ArrayList<>(List.of(id, "S1," + m[0], "S2," + m[1], "S3," + m[2], "S4," + m[3], "S5," + m[4]));
            if (i % 33 == 5) {
                switch ((i / 33) % 4) {
                    case 0 -> { lines.clear(); expectedReasons.merge("empty file", 1L, Long::sum); }
                    case 1 -> { lines.set(0, "ROLL" + i); expectedReasons.merge("bad student id", 1L, Long::sum); }
                    case 2 -> { lines.set(3, "S3,150"); expectedReasons.merge("mark out of range", 1L, Long::sum); }
                    default -> { lines.set(2, "S2,abc"); expectedReasons.merge("malformed line", 1L, Long::sum); }
                }
            } else { expectedOk++; expectedTotals += total; }
            Files.write(p, lines);
        }
    }

    static Callable<Boolean> task(Path p, Summary s, int delay) {
        return () -> {
            try { Result r = Scoring.process(p, delay); s.ok.put(r.file(), r); return true; }
            catch (ProcessingException e) { s.bad.add(new Failure(p.getFileName().toString(), e.getMessage())); return false; }
            catch (RuntimeException e) { s.bad.add(new Failure(p.getFileName().toString(), "unexpected " + e.getClass().getSimpleName())); return false; }
        };
    }

    static Summary runPool(List<Path> fl, int pool, int delay) throws Exception {
        Summary s = new Summary();
        ExecutorService ex = Executors.newFixedThreadPool(pool);
        List<Future<Boolean>> fs = new ArrayList<>();
        for (Path p : fl) fs.add(ex.submit(task(p, s, delay)));
        for (Future<Boolean> f : fs) {
            try { f.get(); } catch (ExecutionException e) { s.lost.increment(); }
        }
        ex.shutdown();
        return s;
    }

    static double ms(long ns) { return ns / 1e6; }

    static long vmHwmKb() throws IOException {
        for (String l : Files.readAllLines(Paths.get("/proc/self/status"))) if (l.startsWith("VmHWM")) return Long.parseLong(l.replaceAll("\\D+", ""));
        return -1;
    }

    public static void main(String[] args) throws Exception {
        generate(Paths.get("scores"), 10_000);

        System.out.println("== correctness on the 10,000-file set ==");
        Summary s = runPool(files, 4, 0);
        System.out.println("succeeded " + s.ok.size() + " (expected " + expectedOk + "), failed " + s.bad.size() + ", lost " + s.lost.sum()
                + ", succeeded + failed + lost = " + (s.ok.size() + s.bad.size() + s.lost.sum()));
        System.out.println("failure reasons found    : " + s.reasons());
        System.out.println("failure reasons injected : " + expectedReasons);
        System.out.println("sum of all totals: " + s.gradeTotal() + " (expected " + expectedTotals + ")");

        System.out.println("\n== pool size vs time, 10,000 files, local disk (best of 5, ms) ==");
        runPool(files, 4, 0); runPool(files, 1, 0);
        for (int p : new int[]{1, 2, 4, 8, 16, 64}) {
            long best = Long.MAX_VALUE;
            for (int k = 0; k < 5; k++) { long t = System.nanoTime(); runPool(files, p, 0); best = Math.min(best, System.nanoTime() - t); }
            System.out.printf("pool %2d : %7.1f ms%n", p, ms(best));
        }

        System.out.println("\n== pool size vs time with a simulated 1 ms wait per file (network share), one run each (ms) ==");
        for (int p : new int[]{1, 2, 4, 8, 16, 64}) {
            long t = System.nanoTime(); Summary r = runPool(files, p, 1000);
            System.out.printf("pool %2d : %8.0f ms   (succeeded %d, failed %d)%n", p, ms(System.nanoTime() - t), r.ok.size(), r.bad.size());
        }

        System.out.println("\n== alternative 1: files.parallelStream() ==");
        for (int delay : new int[]{0, 1000}) {
            Summary ps = new Summary();
            long t = System.nanoTime();
            files.parallelStream().forEach(p -> { try { task(p, ps, delay).call(); } catch (Exception e) { throw new RuntimeException(e); } });
            System.out.printf("%s: %8.1f ms, succeeded %d, failed %d%n", delay == 0 ? "local disk        " : "1 ms simulated wait", ms(System.nanoTime() - t), ps.ok.size(), ps.bad.size());
        }
        System.out.println("common pool parallelism = " + ForkJoinPool.commonPool().getParallelism() + ", cores = " + Runtime.getRuntime().availableProcessors());
        AtomicInteger seen = new AtomicInteger();
        try {
            files.parallelStream().forEach(p -> { try { Scoring.process(p, 0); seen.incrementAndGet(); } catch (ProcessingException e) { throw new RuntimeException(e); } });
        } catch (RuntimeException e) {
            System.out.println("without a catch inside the lambda: run stopped after " + seen.get() + " of 10000 files with " + e.getCause().getMessage());
        }

        System.out.println("\n== alternative 2: one thread per file ==");
        for (int delay : new int[]{0, 1000}) {
            Summary ts = new Summary();
            long before = vmHwmKb();
            java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
            mx.resetPeakThreadCount();
            long t = System.nanoTime();
            List<Thread> threads = new ArrayList<>();
            for (Path p : files) { Thread th = new Thread(() -> { try { task(p, ts, delay).call(); } catch (Exception e) { } }); threads.add(th); th.start(); }
            for (Thread th : threads) th.join();
            System.out.printf("%s: %8.1f ms, succeeded %d, failed %d, peak live threads %d, peak RSS grew by %d MB%n",
                    delay == 0 ? "local disk        " : "1 ms simulated wait", ms(System.nanoTime() - t), ts.ok.size(), ts.bad.size(), mx.getPeakThreadCount(), (vmHwmKb() - before) / 1024);
        }
    }
}
