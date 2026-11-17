import java.util.*;
public class ErrorCodeDemo {
    static int parseCode(String s) {
        if (s == null || s.isEmpty()) return -1;
        int n = s.length(), i = 0;
        if (s.charAt(0) == '-') { if (n == 1) return -1; i = 1; }
        long v = 0;
        for (; i < n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return -1;
            v = v * 10 + (c - '0');
            if (v > Integer.MAX_VALUE) return -1;
        }
        return s.charAt(0) == '-' ? -2 : (int) v;   // -2 = negative amount
    }
    static int parseThrow(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }
    static volatile int sink;
    static long run(boolean useThrow, String s, int n) {
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) sink += useThrow ? parseThrow(s) : parseCode(s);
        return (System.nanoTime() - t) / n;
    }
    public static void main(String[] a) {
        System.out.println("-- error-code style ATM, same seven inputs --");
        String[][] in = {{"1001","2000"},{"10O1","500"},{"1001","-300"},{"1001","9000"},{"9999","100"},{"1001","abc"}};
        for (String[] p : in) {
            int acc = parseCode(p[0]), amt = parseCode(p[1]);
            String r = acc == -1 ? "bad account" : amt == -1 ? "bad amount" : amt == -2 ? "negative amount"
                     : acc == 9999 ? "no account" : amt > 5000 ? "insufficient" : "dispensed";
            System.out.println(p[0] + " / " + p[1] + " -> " + r);
        }
        int N = 2_000_000;
        for (int w = 0; w < 3; w++) { run(true, "abc", 200000); run(false, "abc", 200000); run(true, "12345", 200000); run(false, "12345", 200000); }
        System.out.println("-- cost per call (ns), median of 5 runs --");
        String[] names = {"valid input, exception style", "valid input, error code", "invalid input, exception thrown", "invalid input, error code"};
        boolean[] th = {true, false, true, false};
        String[] sv = {"12345", "12345", "abc", "abc"};
        for (int k = 0; k < 4; k++) {
            long[] r = new long[5];
            for (int j = 0; j < 5; j++) r[j] = run(th[k], sv[k], k >= 2 && th[k] ? 300000 : N);
            Arrays.sort(r);
            System.out.printf("%-34s %6d%n", names[k], r[2]);
        }
    }
}
