import java.util.*;

public class TraceCounter {
    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0, min = 0, max = 0;
    private final List<String> trace = new ArrayList<>();

    private void note(String ev) { min = Math.min(min, count); max = Math.max(max, count); trace.add(ev + "=" + count); }

    public synchronized void put(int token) throws InterruptedException {
        while (count == buffer.length) wait();
        buffer[in] = token; in = (in + 1) % buffer.length; count++;
        note("P"); notifyAll();
    }
    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();
        int t = buffer[out]; out = (out + 1) % buffer.length; count--;
        note("T"); notifyAll(); return t;
    }

    public static void main(String[] args) throws Exception {
        TraceCounter c = new TraceCounter();
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) { } });
        Thread[] st = new Thread[2];
        for (int s = 0; s < 2; s++) st[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) { c.take(); Thread.sleep(2); } } catch (InterruptedException e) { } });
        cook.start(); for (Thread t : st) t.start();
        cook.join(); for (Thread t : st) t.join();
        System.out.println("counter size after each event (P = put, T = take):");
        for (int i = 0; i < c.trace.size(); i += 10) System.out.println("  " + String.join(" ", c.trace.subList(i, Math.min(i + 10, c.trace.size()))));
        System.out.println("smallest size seen = " + c.min + ", largest size seen = " + c.max + ", events = " + c.trace.size());
    }
}
