import java.util.concurrent.*;

public class QueueBench {
    static class Counter {
        final int[] buf = new int[5]; int count, in, out;
        synchronized void put(int t) throws InterruptedException { while (count == 5) wait(); buf[in] = t; in = (in + 1) % 5; count++; notifyAll(); }
        synchronized int take() throws InterruptedException { while (count == 0) wait(); int t = buf[out]; out = (out + 1) % 5; count--; notifyAll(); return t; }
    }
    static long viaWait(int n) throws Exception {
        Counter c = new Counter(); long[] sum = new long[2];
        Thread p = new Thread(() -> { try { for (int i = 1; i <= n; i++) c.put(i); } catch (InterruptedException e) { } });
        Thread[] cs = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; cs[s] = new Thread(() -> { try { for (int i = 0; i < n / 2; i++) sum[id] += c.take(); } catch (InterruptedException e) { } }); }
        long t = System.nanoTime();
        p.start(); for (Thread x : cs) x.start();
        p.join(); for (Thread x : cs) x.join();
        long e = System.nanoTime() - t;
        if (sum[0] + sum[1] != (long) n * (n + 1) / 2) throw new IllegalStateException("bad sum");
        return e;
    }
    static long viaQueue(int n) throws Exception {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5); long[] sum = new long[2];
        Thread p = new Thread(() -> { try { for (int i = 1; i <= n; i++) q.put(i); } catch (InterruptedException e) { } });
        Thread[] cs = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; cs[s] = new Thread(() -> { try { for (int i = 0; i < n / 2; i++) sum[id] += q.take(); } catch (InterruptedException e) { } }); }
        long t = System.nanoTime();
        p.start(); for (Thread x : cs) x.start();
        p.join(); for (Thread x : cs) x.join();
        long e = System.nanoTime() - t;
        if (sum[0] + sum[1] != (long) n * (n + 1) / 2) throw new IllegalStateException("bad sum");
        return e;
    }
    public static void main(String[] a) throws Exception {
        int n = 200_000;
        for (int w = 0; w < 3; w++) { viaWait(n); viaQueue(n); }
        long b1 = Long.MAX_VALUE, b2 = Long.MAX_VALUE;
        for (int k = 0; k < 7; k++) { b1 = Math.min(b1, viaWait(n)); b2 = Math.min(b2, viaQueue(n)); }
        System.out.println("200,000 tokens, 1 producer, 2 consumers, capacity 5");
        System.out.printf("wait()/notifyAll() : %6d ns per token (put + take)%n", b1 / n);
        System.out.printf("ArrayBlockingQueue : %6d ns per token (put + take)%n", b2 / n);
    }
}
