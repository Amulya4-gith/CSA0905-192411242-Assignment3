import java.util.*;

public class PriorityDemo {
    static volatile long sink;
    static List<String> order;
    static Thread make(String name, int prio) {
        Thread t = new Thread(() -> {
            long x = 0;
            for (long i = 0; i < 120_000_000L; i++) x += i ^ (x >> 3);
            sink = x;
            order.add(name);
        }, name);
        t.setPriority(prio);
        return t;
    }
    static String trial(int[] prios) throws InterruptedException {
        order = Collections.synchronizedList(new ArrayList<>());
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) ts[i] = make("S" + (i + 1), prios[i]);
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return String.join(" ", order);
    }
    public static void main(String[] a) throws Exception {
        System.out.println("equal priorities (5,5,5,5), CPU-bound stations:");
        for (int k = 1; k <= 5; k++) System.out.println("  run " + k + ": " + trial(new int[]{5, 5, 5, 5}));
        System.out.println("S1=1, S2=3, S3=7, S4=10:");
        for (int k = 1; k <= 5; k++) System.out.println("  run " + k + ": " + trial(new int[]{1, 3, 7, 10}));
    }
}
