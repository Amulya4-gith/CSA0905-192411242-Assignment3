import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Scale {
    static void close(Closeable c) { try { c.close(); } catch (IOException e) { } }

    static long run(String mode, List<String> files, boolean failLast) {
        Handles.reset();
        long t = System.nanoTime();
        try {
            for (String f : files) {
                if (mode.equals("none")) { new TReader(f).readLine(); }
                else if (mode.equals("finally")) { TReader r = null; try { r = new TReader(f); r.readLine(); } finally { if (r != null) close(r); } }
                else { try (TReader r = new TReader(f)) { r.readLine(); } }
            }
            if (failLast) throw new RuntimeException("late failure");
        } catch (Exception e) { }
        return System.nanoTime() - t;
    }

    public static void main(String[] a) throws Exception {
        Files.createDirectories(Paths.get("many"));
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 6000; i++) { String p = "many/f" + i + ".txt"; if (!Files.exists(Paths.get(p))) Files.writeString(Paths.get(p), "x,1,2\n"); all.add(p); }
        System.out.println("files | version        | time (ms) | max handles open at once | open after a failure at the end");
        for (int f : new int[]{100, 500, 1000}) {
            List<String> files = all.subList(0, f);
            for (String mode : new String[]{"finally", "twr", "none"}) {
                run(mode, files, false);
                long best = Long.MAX_VALUE;
                for (int k = 0; k < 5; k++) best = Math.min(best, run(mode, files, false));
                run(mode, files, true);
                System.out.printf("%5d | %-14s | %9.2f | %24d | %d%n", f, mode.equals("twr") ? "try-with-res" : mode.equals("none") ? "never closed" : "finally close", best / 1e6, Handles.max, Handles.open);
            }
        }
    }
}
