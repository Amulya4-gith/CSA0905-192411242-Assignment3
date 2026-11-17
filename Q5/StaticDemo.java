import java.util.concurrent.atomic.*;

public class StaticDemo {
    static int totalSoldInstanceLock = 0;
    static int totalSoldClassLock = 0;

    static class Screen {
        synchronized void sellInstance() { int v = totalSoldInstanceLock; Thread.yield(); totalSoldInstanceLock = v + 1; }          // locks "this" (one lock per screen)
        static synchronized void sellStatic() { int v = totalSoldClassLock; Thread.yield(); totalSoldClassLock = v + 1; }        // locks Screen.class (one lock for all)
    }

    public static void main(String[] a) throws Exception {
        int screens = 20, perScreen = 20_000;
        Screen[] s = new Screen[screens];
        for (int i = 0; i < screens; i++) s[i] = new Screen();
        Thread[] t = new Thread[screens];
        for (int i = 0; i < screens; i++) { final Screen sc = s[i]; t[i] = new Thread(() -> { for (int k = 0; k < perScreen; k++) sc.sellInstance(); }); t[i].start(); }
        for (Thread x : t) x.join();
        System.out.println("expected total       : " + (screens * perScreen));
        System.out.println("instance-lock result : " + totalSoldInstanceLock + "   (lost " + (screens * perScreen - totalSoldInstanceLock) + ")");
        for (int i = 0; i < screens; i++) { t[i] = new Thread(() -> { for (int k = 0; k < perScreen; k++) Screen.sellStatic(); }); t[i].start(); }
        for (Thread x : t) x.join();
        System.out.println("class-lock result    : " + totalSoldClassLock + "   (lost " + (screens * perScreen - totalSoldClassLock) + ")");
    }
}
