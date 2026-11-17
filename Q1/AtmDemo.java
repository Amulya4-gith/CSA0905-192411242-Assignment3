import java.io.*;
import java.util.*;

public class AtmDemo {
    static final Map<Integer, Integer> balances = new HashMap<>();
    static final String CARD_FILE = "card.txt";

    static String readCard(String path) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            return r.readLine();
        }
    }

    static void withdraw(int account, int amount) throws InsufficientFundsException {
        int balance = balances.get(account);
        if (amount > balance)
            throw new InsufficientFundsException("Balance " + balance + " is less than " + amount);
        balances.put(account, balance - amount);
    }

    static void attempt(String id, String cardFile, String accText, String amtText) {
        String caught = "none";
        String result = "";
        try {
            String holder = readCard(cardFile);
            int account = Integer.parseInt(accText);
            int amount = Integer.parseInt(amtText);
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive: " + amount);
            if (!balances.containsKey(account)) throw new NoSuchElementException("No account " + account);
            withdraw(account, amount);
            result = "Rs " + amount + " dispensed to " + holder;
        } catch (FileNotFoundException e) {
            caught = "FileNotFoundException";
            result = "Card reader file missing";
        } catch (IOException e) {
            caught = "IOException";
            result = "Card reader error";
        } catch (NumberFormatException | NoSuchElementException e) {
            caught = e.getClass().getSimpleName();
            result = "Bad input: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            caught = "IllegalArgumentException";
            result = e.getMessage();
        } catch (InsufficientFundsException e) {
            caught = "InsufficientFundsException";
            result = e.getMessage();
        } finally {
            System.out.printf("%-3s | %-27s | finally ran | %s%n", id, caught, result);
        }
    }

    public static void main(String[] args) throws Exception {
        try (PrintWriter w = new PrintWriter(CARD_FILE)) { w.println("AMULYA"); }
        balances.put(1001, 5000);
        System.out.println("ID  | catch block entered         | finally     | outcome");
        attempt("T1", CARD_FILE, "1001", "2000");
        attempt("T2", CARD_FILE, "10O1", "500");
        attempt("T3", CARD_FILE, "1001", "-300");
        attempt("T4", CARD_FILE, "1001", "9000");
        attempt("T5", "nocard.txt", "1001", "500");
        attempt("T6", CARD_FILE, "9999", "100");
        attempt("T7", CARD_FILE, "1001", "abc");
        System.out.println("Final balance of 1001 = " + balances.get(1001));
    }
}

class InsufficientFundsException extends Exception {
    InsufficientFundsException(String message) { super(message); }
}
