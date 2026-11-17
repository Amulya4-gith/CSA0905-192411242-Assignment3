public class LifeCycle {
    static final Object lock = new Object();
    public static void main(String[] args) throws Exception {
        Thread sleeper = new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException e) { } }, "sleeper");
        System.out.println("sleeper before start(): " + sleeper.getState());
        sleeper.start();
        Thread.sleep(80);
        System.out.println("sleeper while in sleep(300): " + sleeper.getState());

        Thread waiter = new Thread(() -> { try { sleeper.join(); } catch (InterruptedException e) { } }, "waiter");
        waiter.start();
        Thread.sleep(80);
        System.out.println("waiter while in sleeper.join(): " + waiter.getState());

        Thread holder = new Thread(() -> { synchronized (lock) { try { Thread.sleep(300); } catch (InterruptedException e) { } } }, "holder");
        Thread blocked = new Thread(() -> { synchronized (lock) { } }, "blocked");
        holder.start(); Thread.sleep(50);
        blocked.start(); Thread.sleep(50);
        System.out.println("blocked while lock is held by another thread: " + blocked.getState());

        Thread busy = new Thread(() -> { long x = 0; for (long i = 0; i < 400_000_000L; i++) x += i; if (x == 1) System.out.println(); }, "busy");
        busy.start(); Thread.sleep(50);
        System.out.println("busy while computing: " + busy.getState());

        sleeper.join(); waiter.join(); holder.join(); blocked.join(); busy.join();
        System.out.println("sleeper after it ends: " + sleeper.getState());
    }
}
