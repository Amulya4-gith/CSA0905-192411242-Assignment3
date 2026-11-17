import java.util.*;
import java.util.concurrent.*;

public class RaceTest {
    interface Cinema { void book(int seat); int made(); }

    static void pause(int gap) {
        if (gap == 1) Thread.yield();
        else if (gap == 2) { try { Thread.sleep(1); } catch (InterruptedException e) { } }
    }

    static class Unsafe implements Cinema {
        final boolean[] booked = new boolean[10]; int made = 0; final int gap;
        Unsafe(int gap) { this.gap = gap; }
        public void book(int seat) {
            if (!booked[seat]) { pause(gap); booked[seat] = true; made++; }
        }
        public int made() { return made; }
    }
    static class SyncMethod implements Cinema {
        final boolean[] booked = new boolean[10]; int made = 0; final int gap;
        SyncMethod(int gap) { this.gap = gap; }
        public synchronized void book(int seat) {
            if (!booked[seat]) { pause(gap); booked[seat] = true; made++; }
        }
        public int made() { return made; }
    }
    static class SyncBlock implements Cinema {
        final boolean[] booked = new boolean[10]; int made = 0; final int gap; final Object lock = new Object();
        SyncBlock(int gap) { this.gap = gap; }
        public void book(int seat) {
            synchronized (lock) {
                if (!booked[seat]) { pause(gap); booked[seat] = true; made++; }
            }
        }
        public int made() { return made; }
    }

    static int trial(Cinema c, boolean barrier) throws Exception {
        Thread[] users = new Thread[100];
        CyclicBarrier b = new CyclicBarrier(100);
        for (int i = 0; i < 100; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> {
                if (barrier) { try { b.await(); } catch (Exception e) { } }
                c.book(seat);
            });
            users[i].start();
        }
        for (Thread t : users) t.join();
        return c.made();
    }

    static void report(String label, String kind, int gap, boolean barrier, int runs) throws Exception {
        int[] r = new int[runs]; int wrong = 0, max = 0; long sum = 0;
        for (int i = 0; i < runs; i++) {
            Cinema c = kind.equals("unsafe") ? new Unsafe(gap) : kind.equals("method") ? new SyncMethod(gap) : new SyncBlock(gap);
            r[i] = trial(c, barrier);
            if (r[i] != 10) wrong++;
            max = Math.max(max, r[i]); sum += r[i];
        }
        System.out.printf("%-52s runs with sales != 10: %2d/%d   largest count: %d   average: %.1f%n", label, wrong, runs, max, sum / (double) runs);
    }

    public static void main(String[] a) throws Exception {
        System.out.println("-- Student A: trying to reproduce the double booking (unsynchronized) --");
        report("1. plain threads, no gap, no barrier", "unsafe", 0, false, 20);
        report("2. all 100 threads released together (barrier)", "unsafe", 0, true, 20);
        report("3. barrier + Thread.yield() between check and update", "unsafe", 1, true, 20);
        report("4. barrier + sleep(1 ms) between check and update", "unsafe", 2, true, 20);
        System.out.println("\n-- synchronized versions with the same widened gap (barrier + sleep 1 ms) --");
        report("synchronized method", "method", 2, true, 20);
        report("synchronized block", "block", 2, true, 20);
        report("synchronized method, barrier + yield", "method", 1, true, 50);
        report("synchronized block, barrier + yield", "block", 1, true, 50);
    }
}
