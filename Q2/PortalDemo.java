import java.io.*;
import java.time.*;
import java.util.*;

/* ---------- exception hierarchy (all checked): ExamException and SubmissionStorageException are in StorageDemo.java ---------- */
class ExamClosedException extends ExamException {
    ExamClosedException(String m) { super(m); }
}
class NotRegisteredException extends ExamException {
    NotRegisteredException(String m) { super(m); }
}
class FileTooLargeException extends ExamException {
    FileTooLargeException(String m) { super(m); }
}

/* ---------- storage layer ---------- */
class Store {
    void write(String file) throws SubmissionStorageException {
        try {
            if (file.contains("corrupt")) throw new IOException("Input/output error on volume D:");
            System.out.println("    [storage] wrote " + file);
        } catch (IOException e) {
            throw new SubmissionStorageException("Your answer could not be saved, please retry", e);
        }
    }
}

/* ---------- service layer ---------- */
class SubmissionService {
    private final Store store = new Store();
    private final Map<String, Set<String>> registered = new HashMap<>();
    private final LocalDateTime closes = LocalDateTime.of(2026, 11, 20, 12, 0);
    SubmissionService() { registered.put("CSA0905", new HashSet<>(Arrays.asList("192411242", "192411250"))); }

    void submit(String student, String course, String file, long sizeKb, LocalDateTime now)
            throws ExamException {
        if (now.isAfter(closes)) throw new ExamClosedException("Window closed at " + closes);
        Set<String> r = registered.get(course);
        if (r == null || !r.contains(student))
            throw new NotRegisteredException(student + " is not registered for " + course);
        if (sizeKb > 500) throw new FileTooLargeException(sizeKb + " KB is over the 500 KB limit");
        store.write(file);
    }
}

/* ---------- controller layer: the only place that handles ---------- */
public class PortalDemo {
    static final SubmissionService service = new SubmissionService();

    static void controller(String id, String student, String course, String file, long kb, LocalDateTime t) {
        try {
            service.submit(student, course, file, kb, t);
            System.out.println(id + " ACCEPTED: " + file);
        } catch (ExamException e) {
            System.out.println(id + " " + e.getClass().getSimpleName() + " -> student sees: \"" + e.getMessage() + "\""
                    + (e.getCause() != null ? "  [logged cause: " + e.getCause() + "]" : ""));
        }
    }

    public static void main(String[] args) {
        LocalDateTime open = LocalDateTime.of(2026, 11, 20, 10, 30);
        LocalDateTime late = LocalDateTime.of(2026, 11, 20, 12, 5);
        controller("S1", "192411242", "CSA0905", "answer01.pdf", 120, open);
        controller("S2", "192411242", "CSA0905", "answer02.pdf", 120, late);
        controller("S3", "192499999", "CSA0905", "answer03.pdf", 120, open);
        controller("S4", "192411242", "CSA0905", "answer04.pdf", 800, open);
        controller("S5", "192411242", "CSA0905", "corrupt05.pdf", 100, open);

        System.out.println("\n--- full chained stack trace of S5 ---");
        try { service.submit("192411242", "CSA0905", "corrupt05.pdf", 100, open); }
        catch (ExamException e) { e.printStackTrace(System.out); }

        System.out.println("\n--- same failure, re-thrown WITHOUT the cause ---");
        try {
            try { service.submit("192411242", "CSA0905", "corrupt05.pdf", 100, open); }
            catch (SubmissionStorageException e) { throw new ExamException("Your answer could not be saved, please retry"); }
        } catch (ExamException e) {
            System.out.println("getCause() = " + e.getCause());
            e.printStackTrace(System.out);
        }
    }
}
