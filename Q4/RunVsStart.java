public class RunVsStart {
    public static void main(String[] args) throws InterruptedException {
        Runnable job = () -> System.out.println("  job runs on thread: " + Thread.currentThread().getName());
        Thread a = new Thread(job, "Station-A");
        System.out.println("calling a.run():");
        a.run();
        System.out.println("  state of Station-A after run(): " + a.getState());
        Thread b = new Thread(job, "Station-B");
        System.out.println("calling b.start():");
        b.start();
        b.join();
        System.out.println("  state of Station-B after join(): " + b.getState());
        try { b.start(); } catch (IllegalThreadStateException e) { System.out.println("second start() -> " + e.getClass().getSimpleName()); }
    }
}
