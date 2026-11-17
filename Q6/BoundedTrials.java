import java.util.*;

public class BoundedTrials {
    enum Mode { WHILE_NOTIFYALL, IF_NOTIFYALL, WHILE_NOTIFY }

    static class Counter {
        final int[] buf; int count, in, out, violations; final Mode mode;
        Counter(int cap, Mode m) { buf = new int[cap]; mode = m; }
        synchronized void put(int t) throws InterruptedException {
            if (mode == Mode.IF_NOTIFYALL) { if (count == buf.length) wait(); }
            else while (count == buf.length) wait();
            buf[in] = t; in = (in + 1) % buf.length; count++;
            if (count < 0 || count > buf.length) violations++;
            if (mode == Mode.WHILE_NOTIFY) notify(); else notifyAll();
        }
        synchronized int take() throws InterruptedException {
            if (mode == Mode.IF_NOTIFYALL) { if (count == 0) wait(); }
            else while (count == 0) wait();
            int t = buf[out]; out = (out + 1) % buf.length; count--;
            if (count < 0 || count > buf.length) violations++;
            if (mode == Mode.WHILE_NOTIFY) notify(); else notifyAll();
            return t;
        }
    }

    static String trial(Mode m, int cooks, int students, int cap) throws Exception {
        Counter c = new Counter(cap, m);
        int perCook = 200, total = cooks * perCook, perStudent = total / students;
        long[] got = new long[students];
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < cooks; i++) { Thread t = new Thread(() -> { try { for (int k = 1; k <= perCook; k++) c.put(k); } catch (Exception e) { } }); t.setDaemon(true); ts.add(t); }
        for (int i = 0; i < students; i++) { final int id = i; Thread t = new Thread(() -> { try { for (int k = 0; k < perStudent; k++) got[id] += c.take(); } catch (Exception e) { } }); t.setDaemon(true); ts.add(t); }
        for (Thread t : ts) t.start();
        long end = System.currentTimeMillis() + 1500;
        for (Thread t : ts) t.join(Math.max(1, end - System.currentTimeMillis()));
        boolean alive = false;
        for (Thread t : ts) if (t.isAlive()) alive = true;
        long sum = 0; for (long g : got) sum += g;
        long expect = (long) cooks * perCook * (perCook + 1) / 2;
        if (alive) return "HANG";
        if (c.violations > 0) return "count out of range";
        return sum == expect ? "ok" : "wrong sum";
    }

    static void tally(String label, Mode m, int cooks, int students, int cap, int runs) throws Exception {
        Map<String, Integer> r = new TreeMap<>();
        for (int i = 0; i < runs; i++) r.merge(trial(m, cooks, students, cap), 1, Integer::sum);
        System.out.printf("%-58s %s%n", label, r);
    }

    public static void main(String[] a) throws Exception {
        System.out.println("outcome over 30 runs each (1 cook, 2 students, capacity 5, 200 tokens per cook)");
        tally("while + notifyAll (the design in TokenCounter)", Mode.WHILE_NOTIFYALL, 1, 2, 5, 30);
        tally("if + notifyAll", Mode.IF_NOTIFYALL, 1, 2, 5, 30);
        System.out.println();
        System.out.println("outcome over 30 runs each (3 cooks, 3 students, capacity 1, 200 tokens per cook)");
        tally("while + notifyAll", Mode.WHILE_NOTIFYALL, 3, 3, 1, 30);
        tally("while + notify (single wake-up)", Mode.WHILE_NOTIFY, 3, 3, 1, 30);
        System.out.println();
        System.out.println("outcome over 30 runs each (1 cook, 2 students, capacity 5)");
        tally("while + notify (single wake-up), one producer", Mode.WHILE_NOTIFY, 1, 2, 5, 30);
    }
}
