import java.util.ArrayList;
import java.util.List;

public class Statistics {
    private final List<Long> waitTimes;
    private int totalPassengers;
    private int planesServed;

    public Statistics() {
        this.waitTimes = new ArrayList<>();
        this.totalPassengers = 0;
        this.planesServed = 0;
    }

    public synchronized void recordPlane(Plane plane, long waitTime) {
        waitTimes.add(waitTime);
        totalPassengers += plane.getPassengers();
        planesServed++;
    }

    public void printStatistics() {
        if (waitTimes.isEmpty()) {
            System.out.println("No planes served");
            return;
        }

        long maxWait = Long.MIN_VALUE;
        long minWait = Long.MAX_VALUE;
        long totalWait = 0;

        for (long wait : waitTimes) {
            if (wait > maxWait)
                maxWait = wait;
            if (wait < minWait)
                minWait = wait;
            totalWait += wait;
        }

        double avgWait = (double) totalWait / waitTimes.size();

        System.out.println("Number of planes served: " + planesServed);
        System.out.println("Total passengers boarded: " + totalPassengers);
        System.out.println("Maximum wait time: " + maxWait + " ms (" +
                String.format("%.2f", maxWait / 1000.0) + " seconds)");
        System.out.println("Minimum wait time: " + minWait + " ms (" +
                String.format("%.2f", minWait / 1000.0) + " seconds)");
        System.out.println("Average wait time: " +
                String.format("%.2f", avgWait) + " ms (" +
                String.format("%.2f", avgWait / 1000.0) + " seconds)");
    }
}