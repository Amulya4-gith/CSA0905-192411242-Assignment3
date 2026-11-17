public class UncaughtDemo {
    static int parseAmount(String s) { return Integer.parseInt(s); }
    static void withdraw(String s) { int a = parseAmount(s); System.out.println(a); }
    public static void main(String[] args) {
        withdraw("12x");
    }
}
