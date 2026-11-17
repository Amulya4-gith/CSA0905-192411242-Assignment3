import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Scan {
    static final AtomicInteger total = new AtomicInteger();
    static final List<String> order = Collections.synchronizedList(new ArrayList<>());
    static void batch(String station, int scripts) {
        try {
            for (int i = 0; i < scripts; i++) Thread.sleep(1);     // one script takes about 1 ms to scan
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        total.addAndGet(scripts);
        order.add(station);
    }
}

class StationThread extends Thread {
    private final int scripts;
    StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
    @Override public void run() { Scan.batch(getName(), scripts); }
}

class StationTask implements Runnable {
    private final int scripts;
    StationTask(int scripts) { this.scripts = scripts; }
    @Override public void run() { Scan.batch(Thread.currentThread().getName(), scripts); }
}

public class ScanDemo {
    public static void main(String[] args) throws InterruptedException {
        boolean useThread = args.length > 0 && args[0].equals("thread");
        Thread[] st = new Thread[4];
        for (int i = 0; i < 4; i++) {
            String name = "S" + (i + 1);
            st[i] = useThread ? new StationThread(name, 100) : new Thread(new StationTask(100), name);
        }
        for (Thread t : st) t.start();
        System.out.println("alive right after start(): " + st[0].isAlive() + " " + st[1].isAlive() + " " + st[2].isAlive() + " " + st[3].isAlive());
        for (Thread t : st) t.join();
        System.out.println("alive after join():        " + st[0].isAlive() + " " + st[1].isAlive() + " " + st[2].isAlive() + " " + st[3].isAlive());
        System.out.println("finish order: " + String.join(" ", Scan.order));
        System.out.println("total scanned: " + Scan.total.get());
    }
}
