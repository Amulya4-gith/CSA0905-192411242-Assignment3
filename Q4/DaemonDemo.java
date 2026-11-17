public class DaemonDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread normal = new Thread(() -> { sleep(300); System.out.println("S1 (normal) finished its batch"); }, "S1");
        Thread daemon = new Thread(() -> { sleep(2000); System.out.println("S4 (daemon) finished its batch"); }, "S4");
        daemon.setDaemon(true);
        normal.start(); daemon.start();
        System.out.println("S4 isDaemon = " + daemon.isDaemon());
        normal.join();
        System.out.println("main() ends now; S4 isAlive = " + daemon.isAlive());
    }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }
}
