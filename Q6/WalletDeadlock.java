import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

class Wallet {
    final int id; int balance; final ReentrantLock lock = new ReentrantLock();
    Wallet(int id, int balance) { this.id = id; this.balance = balance; }
}

public class WalletDeadlock {
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }

    /* can deadlock: locks "from" first, then "to" */
    static void transferBad(Wallet from, Wallet to, int amt, long pause) {
        synchronized (from) {
            sleep(pause);
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }
    /* cannot deadlock: always lock the wallet with the smaller id first */
    static void transferOrdered(Wallet from, Wallet to, int amt) {
        Wallet first = from.id < to.id ? from : to, second = first == from ? to : from;
        synchronized (first) { synchronized (second) { from.balance -= amt; to.balance += amt; } }
    }
    /* alternative: tryLock with a timeout, retry on failure */
    static int retries = 0;
    static boolean transferTry(Wallet from, Wallet to, int amt) throws InterruptedException {
        if (from.lock.tryLock(1, TimeUnit.MILLISECONDS)) {
            try {
                if (to.lock.tryLock(1, TimeUnit.MILLISECONDS)) {
                    try { from.balance -= amt; to.balance += amt; return true; } finally { to.lock.unlock(); }
                }
            } finally { from.lock.unlock(); }
        }
        return false;
    }

    static Thread daemon(String name, Runnable r) { Thread t = new Thread(r, name); t.setDaemon(true); return t; }

    public static void main(String[] args) throws Exception {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();

        System.out.println("== Student B: a pair of transfers that deadlocks ==");
        Wallet a = new Wallet(1, 1000), b = new Wallet(2, 1000);
        Thread t1 = daemon("Transfer-1 (A to B)", () -> transferBad(a, b, 100, 100));
        Thread t2 = daemon("Transfer-2 (B to A)", () -> transferBad(b, a, 50, 100));
        t1.start(); t2.start();
        t1.join(600); t2.join(600);
        System.out.println("after 600 ms: t1 alive = " + t1.isAlive() + ", t2 alive = " + t2.isAlive());
        long[] ids = mx.findDeadlockedThreads();
        System.out.println("findDeadlockedThreads() found " + (ids == null ? 0 : ids.length) + " threads");
        if (ids != null) for (ThreadInfo ti : mx.getThreadInfo(ids))
            System.out.println("  " + ti.getThreadName() + " is " + ti.getThreadState() + ", wants a lock owned by " + ti.getLockOwnerName());
        System.out.println("balances unchanged: A = " + a.balance + ", B = " + b.balance);

        System.out.println("\n== same two transfers with lock ordering ==");
        Wallet c = new Wallet(1, 1000), d = new Wallet(2, 1000);
        Thread u1 = daemon("Transfer-1", () -> { sleep(100); transferOrdered(c, d, 100); });
        Thread u2 = daemon("Transfer-2", () -> { sleep(100); transferOrdered(d, c, 50); });
        u1.start(); u2.start(); u1.join(2000); u2.join(2000);
        System.out.println("finished: " + (!u1.isAlive() && !u2.isAlive()) + ", C = " + c.balance + ", D = " + d.balance + ", total = " + (c.balance + d.balance));

        System.out.println("\n== random transfers, no pause, 4 threads x 5000 transfers among 4 wallets, 20 trials ==");
        for (String kind : new String[]{"unordered", "ordered"}) {
            int stuck = 0;
            for (int trial = 0; trial < 20; trial++) {
                Wallet[] w = new Wallet[4];
                for (int i = 0; i < 4; i++) w[i] = new Wallet(i, 1000);
                Thread[] ts = new Thread[4];
                for (int i = 0; i < 4; i++) {
                    final long seed = trial * 31L + i;
                    ts[i] = daemon("w" + i, () -> {
                        Random r = new Random(seed);
                        for (int k = 0; k < 5000; k++) {
                            int x = r.nextInt(4), y = r.nextInt(4); if (x == y) continue;
                            if (kind.equals("ordered")) transferOrdered(w[x], w[y], 1); else transferBad(w[x], w[y], 1, 0);
                            if (k % 50 == 0) Thread.yield();
                        }
                    });
                    ts[i].start();
                }
                long end = System.currentTimeMillis() + 1500;
                for (Thread t : ts) t.join(Math.max(1, end - System.currentTimeMillis()));
                boolean alive = false; for (Thread t : ts) if (t.isAlive()) alive = true;
                if (alive) stuck++;
                else { int sum = 0; for (Wallet x : w) sum += x.balance; if (sum != 4000) System.out.println("MONEY LOST: " + sum); }
            }
            System.out.println(kind + " locking: deadlocked in " + stuck + " of 20 trials");
        }

        System.out.println("\n== cost per transfer: 4 threads x 50,000 transfers among 10 wallets (ns, best of 5) ==");
        for (String kind : new String[]{"ordered synchronized", "tryLock + retry"}) {
            long best = Long.MAX_VALUE; long lastRetries = 0;
            for (int rep = 0; rep < 6; rep++) {
                Wallet[] w = new Wallet[10];
                for (int i = 0; i < 10; i++) w[i] = new Wallet(i, 1000);
                java.util.concurrent.atomic.AtomicLong fails = new java.util.concurrent.atomic.AtomicLong();
                Thread[] ts = new Thread[4];
                long t0 = System.nanoTime();
                for (int i = 0; i < 4; i++) {
                    final long seed = 100 + i;
                    ts[i] = new Thread(() -> {
                        Random r = new Random(seed);
                        try {
                            for (int k = 0; k < 50_000; k++) {
                                int x = r.nextInt(10), y = r.nextInt(10); if (x == y) continue;
                                if (kind.startsWith("ordered")) transferOrdered(w[x], w[y], 1);
                                else while (!transferTry(w[x], w[y], 1)) fails.incrementAndGet();
                            }
                        } catch (InterruptedException e) { }
                    });
                    ts[i].start();
                }
                for (Thread t : ts) t.join();
                long dt = System.nanoTime() - t0;
                if (rep > 0) best = Math.min(best, dt);
                lastRetries = fails.get();
            }
            System.out.printf("%-22s %6d ns per transfer   (retries in last run: %d)%n", kind, best / 200_000, lastRetries);
        }
    }
}
