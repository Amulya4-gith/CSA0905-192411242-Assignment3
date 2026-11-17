import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HandCheck {
    static void w(Path d, String name, String... lines) throws IOException { Files.write(d.resolve(name), Arrays.asList(lines)); }

    /** second pass in file-name order: a student id that appears in two accepted files keeps only the first */
    static void dedupe(Summary s) {
        Set<String> seen = new HashSet<>();
        for (String f : new TreeSet<>(s.ok.keySet())) {
            Result r = s.ok.get(f);
            if (!seen.add(r.student())) { s.ok.remove(f); s.bad.add(new Failure(f, "duplicate student id")); }
        }
    }

    public static void main(String[] args) throws Exception {
        Path d = Paths.get("hand");
        if (Files.exists(d)) try (var st = Files.walk(d)) { st.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        Files.createDirectories(d);
        w(d, "f01.txt", "STU00001", "S1,90", "S2,80", "S3,70", "S4,60", "S5,50");
        w(d, "f02.txt", "STU00002", "S1,100", "S2,100", "S3,100", "S4,100", "S5,100");
        w(d, "f03.txt", "STU00003", "S1,0", "S2,0", "S3,0", "S4,0", "S5,0");
        w(d, "f04.txt", "STU00004", "S1,45", "S2,50", "S3,55", "S4,40", "S5,35");
        w(d, "f05.txt", "STU00005", "S1,75", "S2,75", "S3,75", "S4,75", "S5,75");
        w(d, "f06.txt");                                                                     // empty
        w(d, "f07.txt", "ROLL7", "S1,50", "S2,50", "S3,50", "S4,50", "S5,50");               // bad id
        w(d, "f08.txt", "STU00008", "S1,50", "S2,101", "S3,50", "S4,50", "S5,50");           // mark 101
        /* f09.txt is never created: missing */
        Files.createDirectories(d.resolve("f10.txt"));                                       // a directory, not a file
        Files.write(d.resolve("f11.txt"), new byte[]{(byte) 0xC3, (byte) 0x28, (byte) 0xFF, (byte) 0xFE, 0x0A});   // invalid UTF-8
        w(d, "f12.txt", "STU00001", "S1,10", "S2,10", "S3,10", "S4,10", "S5,10");            // same student as f01
        w(d, "f13.txt", "STU00013", "S1,50", "S2,50", "S3,50", "S4,50");                     // four subjects only
        List<Path> fl = new ArrayList<>();
        for (int i = 1; i <= 13; i++) fl.add(d.resolve(String.format("f%02d.txt", i)));

        Summary s = Processor.runPool(fl, 4, 0);
        System.out.println("== first run, without a duplicate check ==");
        System.out.println("hand count: 5 valid files, 8 failures (empty, bad id, mark 101, missing, directory, invalid bytes, duplicate, four subjects)");
        System.out.println("program   : " + s.ok.size() + " succeeded, " + s.bad.size() + " failed  ->  " + (s.ok.size() == 5 ? "matches" : "DOES NOT MATCH"));
        dedupe(s);
        System.out.println("\n== after adding the duplicate pass ==");
        System.out.println("program   : " + s.ok.size() + " succeeded, " + s.bad.size() + " failed  ->  " + (s.ok.size() == 5 && s.bad.size() == 8 ? "matches" : "DOES NOT MATCH"));
        System.out.println("succeeded + failed = " + (s.ok.size() + s.bad.size()) + " of 13 files");
        System.out.println("\nresults (hand-calculated total and grade in brackets):");
        String[] hand = {"f01.txt 350 B", "f02.txt 500 A", "f03.txt 0 F", "f04.txt 225 C", "f05.txt 375 A"};
        for (String h : hand) {
            String[] p = h.split(" ");
            Result r = s.ok.get(p[0]);
            System.out.printf("  %s %s total %d grade %s   [%s %s]  %s%n", r.file(), r.student(), r.total(), r.grade(), p[1], p[2],
                    (r.total() == Integer.parseInt(p[1]) && r.grade().equals(p[2])) ? "ok" : "WRONG");
        }
        System.out.println("failures:");
        List<Failure> fx = new ArrayList<>(s.bad);
        fx.sort(Comparator.comparing(Failure::file));
        for (Failure f : fx) System.out.println("  " + f.file() + " -> " + f.reason());
        System.out.println("failure reasons: " + s.reasons());
    }
}
