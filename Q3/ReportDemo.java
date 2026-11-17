import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

class Handles {
    static int open = 0, max = 0;
    static boolean trace = false;
    static void inc(String what) { open++; max = Math.max(max, open); if (trace) System.out.println("    opened " + what + " -> open handles = " + open); }
    static void dec(String what) { open--; if (trace) System.out.println("    closed " + what + " -> open handles = " + open); }
    static void reset() { open = 0; max = 0; }
}

class TReader extends BufferedReader {
    private final String name; private boolean closed;
    TReader(String path) throws FileNotFoundException { super(new FileReader(path)); name = path; Handles.inc(path); }
    @Override public void close() throws IOException { if (!closed) { closed = true; Handles.dec(name); } super.close(); }
}

class TWriter extends PrintWriter {
    private final String name; private boolean closed;
    TWriter(String path) throws FileNotFoundException { super(path); name = path; Handles.inc(path); }
    @Override public void close() { if (!closed) { closed = true; Handles.dec(name); } super.close(); }
}

public class ReportDemo {
    static final Logger LOG = Logger.getLogger("report");

    static double storeTotal(BufferedReader r) throws IOException {
        double total = 0; String line;
        while ((line = r.readLine()) != null) total += Double.parseDouble(line.split(",")[2]);
        return total;
    }

    /* Version 1: manual close(), but close() sits after the read, not in finally */
    static void manualLeaky(List<String> files, String out) throws IOException {
        TWriter w = new TWriter(out);
        double grand = 0;
        for (String f : files) {
            TReader r = new TReader(f);
            double t = storeTotal(r);
            r.close();
            w.println(f + "," + t); grand += t;
        }
        w.println("TOTAL," + grand);
        w.close();
    }

    /* Version 2: manual close() done properly in finally */
    static void manualFinally(List<String> files, String out) throws IOException {
        TWriter w = null;
        try {
            w = new TWriter(out);
            double grand = 0;
            for (String f : files) {
                TReader r = null;
                try {
                    r = new TReader(f);
                    double t = storeTotal(r);
                    w.println(f + "," + t); grand += t;
                } finally {
                    if (r != null) { try { r.close(); } catch (IOException ignored) { } }
                }
            }
            w.println("TOTAL," + grand);
        } finally {
            if (w != null) w.close();
        }
    }

    /* Version 3: try-with-resources, with logging of the failing file */
    static void withResources(List<String> files, String out) throws IOException {
        try (TWriter w = new TWriter(out)) {
            double grand = 0;
            for (String f : files) {
                try (TReader r = new TReader(f)) {
                    double t = storeTotal(r);
                    w.println(f + "," + t); grand += t;
                } catch (RuntimeException e) {
                    LOG.severe("Failed while reading " + f + ": " + e);
                    throw e;
                }
            }
            w.println("TOTAL," + grand);
        }
    }

    interface Job { void run(List<String> files, String out) throws IOException; }

    static void attempt(String label, Job j, List<String> files, String out) {
        Handles.reset();
        String result;
        try { j.run(files, out); result = "completed"; }
        catch (Exception e) { result = "FAILED with " + e.getClass().getSimpleName(); }
        System.out.printf("%-22s %-32s handles still open: %d (max at once %d)%n", label, result, Handles.open, Handles.max);
    }

    static void write(String p, String... lines) throws IOException { Files.write(Paths.get(p), Arrays.asList(lines)); }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get("data"));
        write("data/store1.csv", "pen,10,50.0", "book,2,300.0");
        write("data/store2.csv", "bag,1,700.0", "pen,5,25.0", "ink,3,90.0");
        write("data/store3.csv", "cap,2,120.0", "pen,4,20.0", "ink,3,abc", "bag,1,700.0");   // line 3 is corrupt
        write("data/store4.csv", "pen,8,40.0", "book,1,150.0");
        List<String> good = List.of("data/store1.csv", "data/store2.csv", "data/store4.csv");
        List<String> bad  = List.of("data/store1.csv", "data/store2.csv", "data/store3.csv", "data/store4.csv");

        System.out.println("== good input: all three versions ==");
        attempt("manual (no finally)", ReportDemo::manualLeaky, good, "good_leaky.csv");
        attempt("manual (finally)", ReportDemo::manualFinally, good, "good_finally.csv");
        attempt("try-with-resources", ReportDemo::withResources, good, "good_twr.csv");
        byte[] a = Files.readAllBytes(Paths.get("good_leaky.csv"));
        byte[] b = Files.readAllBytes(Paths.get("good_finally.csv"));
        byte[] c = Files.readAllBytes(Paths.get("good_twr.csv"));
        System.out.println("reports identical: " + (Arrays.equals(a, b) && Arrays.equals(b, c)));
        System.out.println(new String(c).trim());

        System.out.println("\n== bad input (store3.csv line 3 is corrupt) ==");
        attempt("manual (no finally)", ReportDemo::manualLeaky, bad, "bad_leaky.csv");
        attempt("manual (finally)", ReportDemo::manualFinally, bad, "bad_finally.csv");
        attempt("try-with-resources", ReportDemo::withResources, bad, "bad_twr.csv");

        System.out.println("\n== step trace of the leaking version on the bad input ==");
        Handles.trace = true; Handles.reset();
        try { manualLeaky(bad, "trace.csv"); } catch (Exception e) { System.out.println("    exception: " + e); }
        System.out.println("    open handles after failure = " + Handles.open);
        Handles.trace = false;

        System.out.println("\n== partial report left by the failed leaky run ==");
        System.out.println(Files.readString(Paths.get("bad_leaky.csv")).trim().isEmpty() ? "(file is empty: buffered data was never flushed)" : Files.readString(Paths.get("bad_leaky.csv")).trim());
        System.out.println("-- partial report left by try-with-resources --");
        System.out.println(Files.readString(Paths.get("bad_twr.csv")).trim());

        System.out.println("\n== closing order and suppressed exceptions ==");
        class Res implements AutoCloseable {
            final String n; final boolean failOnClose;
            Res(String n, boolean f) { this.n = n; failOnClose = f; System.out.println("opening " + n); }
            public void close() { System.out.println("closing " + n); if (failOnClose) throw new IllegalStateException("close failed for " + n); }
        }
        try (Res first = new Res("A", false); Res second = new Res("B", true)) {
            throw new RuntimeException("body failed");
        } catch (Exception e) {
            System.out.println("caught: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("suppressed: " + s.getMessage());
        }
    }
}
