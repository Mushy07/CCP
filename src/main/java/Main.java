import java.util.Random;

public class Main {
    public static void main(String[] args) {
        System.out.println(Utils.threadInfo() + "Starting Asia Pacific Airport Simulation...\n");

        // Create airport with 3 gates (last one for emergency)
        Airport airport = new Airport(3);

        // Start ATC thread
        Thread atcThread = new Thread(airport, "ATC");
        atcThread.start();

        Random rand = new Random();
        Thread[] planeThreads = new Thread[6];

        // Launch 6 planes
        for (int i = 1; i <= 6; i++) {
            int passengers = rand.nextInt(51); // 0-50 passengers

            // Let the plane thread decide if it's an emergency (not predetermined)
            Plane plane = new Plane(i, passengers, airport);
            Thread planeThread = new Thread(plane, "PLANE-" + i);
            planeThreads[i - 1] = planeThread;
            planeThread.start();

            // Random delay before next plane (1 or 2 seconds)
            if (i < 6) {
                try {
                    Thread.sleep((1 + rand.nextInt(2)) * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Wait for all plane threads to finish
        for (Thread planeThread : planeThreads) {
            try {
                planeThread.join();
            } catch (InterruptedException e) {
                System.err.println(Utils.threadInfo() + "Interrupted while waiting for planes");
                Thread.currentThread().interrupt();
            }
        }

        // Stop ATC
        airport.shutdown();
        try {
            atcThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print statistics
        System.out.println("\n" + Utils.threadInfo() + "========== SIMULATION COMPLETE ==========");
        airport.printStatistics();
    }
}