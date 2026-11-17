public class SingleCatch {
    public static void main(String[] args) {
        String[] inputs = {"500", "abc", null};
        for (String s : inputs) {
            try {
                System.out.println("Dispensing " + Integer.parseInt(s));
            } catch (Exception e) {
                System.out.println("Something went wrong for [" + s + "] -> " + e.getClass().getSimpleName());
            }
        }
    }
}
