import java.io.*;
import java.nio.file.*;
import java.util.*;

public class OsLimit {
    public static void main(String[] a) throws Exception {
        List<FileReader> keep = new ArrayList<>();
        int i = 0;
        try {
            for (; i < 6000; i++) keep.add(new FileReader("many/f" + i + ".txt"));
            System.out.println("opened all 6000");
        } catch (IOException e) {
            System.out.println("file number " + (i + 1) + " failed: " + e.getMessage());
        }
    }
}
