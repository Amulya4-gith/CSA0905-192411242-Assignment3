import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class SyncBench {
    interface Seats { boolean book(int seat); }

    static class SyncMethod implements Seats {
        final boolean[] b; SyncMethod(int s) { b = new boolean[s]; }
        public synchronized boolean book(int i) { if (b[i]) return false; b[i] = true; return true; }
    }
    static class SyncBlock implements Seats {
        final boolean[] b; final Object lock = new Object(); SyncBlock(int s) { b = new boolean[s]; }
        public boolean book(int i) { synchronized (lock) { if (b[i]) return false; b[i] = true; return true; } }
    }
    static class AtomicSeats implements Seats {
        final AtomicBoolean[] b; AtomicSeats(int s) { b = new AtomicBoolean[s]; for (int i = 0; i < s; i++) b[i] = new AtomicBoolean(); }
        public boolean book(int i) { return b[i].compareAndSet(false, true); }
    }
    static class LockSeats implements Seats {
        final boolean[] b; final ReentrantLock l = new ReentrantLock(); LockSeats(int s) { b = new boolean[s]; }
        public boolean book(int i) { l.lock(); try { if (b[i]) return false; b[i] = true; return true; } finally { l.unlock(); } }
    }
    static class MapSeats implements Seats {
        final ConcurrentHashMap<Integer, Boolean> m = new ConcurrentHashMap<>(); MapSeats(int s) { }
        public boolean book(int i) { return m.putIfAbsent(i, Boolean.TRUE) == null; }
    }
    /* whole method locked, with 50 microseconds of non-critical work (validation, receipt) inside */
    static class WorkMethod implements Seats {
        final boolean[] b; WorkMethod(int s) { b = new boolean[s]; }
        public synchronized boolean book(int i) { work(); if (b[i]) return false; b[i] = true; return true; }
    }
    /* only the check-and-update locked; the same work done outside */
    static class WorkBlock implements Seats {
        final boolean[] b; final Object lock = new Object(); WorkBlock(int s) { b = new boolean[s]; }
        public boolean book(int i) { work(); synchronized (lock) { if (b[i]) return false; b[i] = true; return true; } }
    }
    static void work() { try { Thread.sleep(0, 50_000); } catch (InterruptedException e) { } }

    static long[] run(Seats s, int threads, int per, int seats, int[][] plan) throws Exception {
        AtomicInteger sold = new AtomicInteger();
        Thread[] ts = new Thread[threads];
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            final int[] p = plan[t];
            ts[t] = new Thread(() -> {
                try { go.await(); } catch (InterruptedException e) { }
                int c = 0;
                for (int x : p) if (s.book(x)) c++;
                sold.addAndGet(c);
            });
            ts[t].start();
        }
        long t0 = System.nanoTime();
        go.countDown();
        for (Thread t : ts) t.join();
        return new long[]{System.nanoTime() - t0, sold.get()};
    }

    static int[][] plan(int threads, int per, int seats, long seed) {
        Random r = new Random(seed);
        int[][] p = new int[threads][per];
        for (int[] row : p) for (int i = 0; i < per; i++) row[i] = r.nextInt(seats);
        return p;
    }

    static long best(java.util.function.IntFunction<Seats> f, int threads, int per, int seats) throws Exception {
        int[][] pl = plan(threads, per, seats, 7);
        long best = Long.MAX_VALUE;
        for (int k = 0; k < 7; k++) {
            long[] r = run(f.apply(seats), threads, per, seats, pl);
            if (k >= 2) best = Math.min(best, r[0]);
        }
        return best;
    }

    static final com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long alloc(java.util.function.IntFunction<Seats> f, int seats, boolean fill) {
        long id = Thread.currentThread().getId();
        long b0 = mx.getThreadAllocatedBytes(id);
        Seats s = f.apply(seats);
        if (fill) for (int i = 0; i < seats; i++) s.book(i);
        return mx.getThreadAllocatedBytes(id) - b0;
    }

    public static void main(String[] a) throws Exception {
        String[] names = {"synchronized method", "synchronized block", "AtomicBoolean[] CAS", "ReentrantLock", "ConcurrentHashMap"};
        java.util.function.IntFunction<Seats>[] mk = new java.util.function.IntFunction[]{
            (java.util.function.IntFunction<Seats>) SyncMethod::new, SyncBlock::new, AtomicSeats::new, LockSeats::new, MapSeats::new};
        System.out.println("-- 10,000 booking attempts, 1000 seats, pure critical section (time in microseconds, best of 5) --");
        System.out.println("strategy              | 1 thread | 4 threads | 8 threads");
        for (int k = 0; k < 5; k++) {
            long t1 = best(mk[k], 1, 10000, 1000), t4 = best(mk[k], 4, 2500, 1000), t8 = best(mk[k], 8, 1250, 1000);
            System.out.printf("%-21s | %8d | %9d | %9d%n", names[k], t1 / 1000, t4 / 1000, t8 / 1000);
        }
        System.out.println();
        System.out.println("-- 10,000 bookings, 8 threads, with 50 us of non-critical work per booking (ms, best of 3) --");
        int[][] pl = plan(8, 1250, 1000, 11);
        for (String nm : new String[]{"method", "block"}) {
            long best = Long.MAX_VALUE;
            for (int k = 0; k < 3; k++) {
                long[] r = run(nm.equals("method") ? new WorkMethod(1000) : new WorkBlock(1000), 8, 1250, 1000, pl);
                best = Math.min(best, r[0]);
            }
            System.out.printf("lock whole method: %-5s -> %d ms%n", nm.equals("method") ? "yes" : "no (block)", best / 1_000_000);
        }
        System.out.println();
        System.out.println("-- extra space for s = 100,000 seats (bytes allocated to build the structure) --");
        for (int k = 0; k < 5; k++) {
            alloc(mk[k], 100000, true);
            System.out.printf("%-21s | empty: %9d | all seats booked: %9d%n", names[k], alloc(mk[k], 100000, false), alloc(mk[k], 100000, true));
        }
    }
}
