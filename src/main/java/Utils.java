public class Utils {
    
    /**
     * Returns thread information in the format: [ThreadName] :
     * This ensures all output shows which thread is performing the action
     */
    public static String threadInfo() {
        return "[" + Thread.currentThread().getName() + "] : ";
    }
    
    /**
     * Sleep wrapper that handles InterruptedException
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(threadInfo() + "Sleep interrupted");
        }
    }
}