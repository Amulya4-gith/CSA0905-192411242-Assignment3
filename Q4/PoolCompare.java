import java.util.*;
import java.util.concurrent.*;

public class PoolCompare {
    static void sleepTask() { try { Thread.sleep(10); } catch (InterruptedException e) { } }

    static long viaPool(int n, int p) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(p);
        long t = System.nanoTime();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) fs.add(ex.submit(PoolCompare::sleepTask));
        for (Future<?> f : fs) f.get();
        long e = System.nanoTime() - t;
        ex.shutdown();
        return e / 1_000_000;
    }
    static long viaThreads(int n) throws Exception {
        long t = System.nanoTime();
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) { ts[i] = new Thread(PoolCompare::sleepTask); ts[i].start(); }
        for (Thread x : ts) x.join();
        return (System.nanoTime() - t) / 1_000_000;
    }
    public static void main(String[] a) throws Exception {
        int n = 200;
        viaPool(n, 4); viaThreads(n);
        System.out.println("n = 200 tasks of 10 ms each (ideal time = n/p x 10 ms)");
        System.out.println("pool size p | measured (ms) | ideal (ms)");
        for (int p : new int[]{1, 2, 4, 8, 16, 50}) {
            long best = Long.MAX_VALUE;
            for (int k = 0; k < 3; k++) best = Math.min(best, viaPool(n, p));
            System.out.printf("%11d | %13d | %d%n", p, best, (n + p - 1) / p * 10);
        }
        long bt = Long.MAX_VALUE;
        for (int k = 0; k < 3; k++) bt = Math.min(bt, viaThreads(n));
        System.out.println("one new Thread per task (200 threads): " + bt + " ms");
        System.out.println();
        System.out.println("thread cost: 5000 trivial tasks");
        Runnable noop = () -> { };
        long best1 = Long.MAX_VALUE, best2 = Long.MAX_VALUE;
        for (int k = 0; k < 5; k++) {
            long t = System.nanoTime();
            Thread[] ts = new Thread[5000];
            for (int i = 0; i < 5000; i++) { ts[i] = new Thread(noop); ts[i].start(); }
            for (Thread x : ts) x.join();
            best1 = Math.min(best1, (System.nanoTime() - t) / 1000);
            ExecutorService ex = Executors.newFixedThreadPool(4);
            t = System.nanoTime();
            List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < 5000; i++) fs.add(ex.submit(noop));
            for (Future<?> f : fs) f.get();
            best2 = Math.min(best2, (System.nanoTime() - t) / 1000);
            ex.shutdown();
        }
        System.out.printf("5000 new Threads: %.1f ms;  pool of 4: %.1f ms%n", best1 / 1000.0, best2 / 1000.0);
    }
}
