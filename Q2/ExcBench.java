import java.lang.management.ManagementFactory;

public class ExcBench {
    static class Plain extends Exception {
        Plain(String m, Throwable c) { super(m, c); }
    }
    static class NoTrace extends Exception {
        NoTrace(String m, Throwable c) { super(m, c, false, false); }
    }
    static volatile Object sink;
    static final com.sun.management.ThreadMXBean mx =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static class QuietCause extends Exception {
        QuietCause(String m) { super(m, null, false, false); }
    }
    static void deep(int d, boolean trace, boolean chain) throws Exception {
        if (d > 0) { deep(d - 1, trace, chain); return; }
        Throwable cause = chain ? (trace ? new java.io.IOException("disk") : new QuietCause("disk")) : null;
        throw trace ? new Plain("x", cause) : new NoTrace("x", cause);
    }
    static int errorCode(int d) { return d > 0 ? errorCode(d - 1) : -1; }

    static long time(int depth, boolean trace, boolean chain, int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) { try { deep(depth, trace, chain); } catch (Exception e) { sink = e; } }
        return (System.nanoTime() - t) / n;
    }
    static long bytes(int depth, boolean trace, boolean chain) {
        long id = Thread.currentThread().getId();
        long b = mx.getThreadAllocatedBytes(id);
        for (int i = 0; i < 1000; i++) { try { deep(depth, trace, chain); } catch (Exception e) { sink = e; } }
        return (mx.getThreadAllocatedBytes(id) - b) / 1000;
    }
    public static void main(String[] a) {
        for (int w = 0; w < 5; w++) { time(10, true, true, 20000); time(10, false, true, 20000); time(100, true, true, 5000); }
        System.out.println("depth | stack trace on | cause | ns per throw+catch | bytes allocated");
        int[] depths = {10, 50, 200};
        for (int d : depths) {
            for (int k = 0; k < 3; k++) {
                boolean trace = k != 0;
                boolean chain = k == 2;
                long[] r = new long[5];
                for (int j = 0; j < 5; j++) r[j] = time(d, trace, chain, d >= 200 ? 3000 : 20000);
                java.util.Arrays.sort(r);
                System.out.printf("%5d | %-14s | %-5s | %18d | %d%n", d, trace ? "yes" : "no", chain ? "yes" : "no", r[2], bytes(d, trace, chain));
            }
        }
        long t = System.nanoTime();
        int s = 0;
        for (int i = 0; i < 20_000_000; i++) s += errorCode(10);
        System.out.println("error code return, depth 10: " + (System.nanoTime() - t) / 20_000_000.0 + " ns (" + s + ")");
    }
}
