public class BroaderFirst {
    public static void main(String[] args) {
        try {
            Integer.parseInt("abc");
        } catch (Exception e) {
            System.out.println("general");
        } catch (NumberFormatException e) {
            System.out.println("specific");
        }
    }
}
