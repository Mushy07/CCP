import java.util.Random;

public class Plane implements Runnable {
        private final int id;
        private final int passengers;
        private final Airport airport;
        private boolean isEmergency;
        private final long arrivalTime;

        public Plane(int id, int passengers, Airport airport) {
                this.id = id;
                this.passengers = passengers;
                this.airport = airport;
                this.arrivalTime = System.currentTimeMillis();
                // Emergency status will be determined by the plane thread itself
                this.isEmergency = false;
        }

        public int getId() {
                return id;
        }

        public int getPassengers() {
                return passengers;
        }

        public boolean isEmergency() {
                return isEmergency;
        }

        public long getArrivalTime() {
                return arrivalTime;
        }

        @Override
        public void run() {
                try {
                        // Each plane thread randomly decides if it has an emergency (20% chance)
                        Random rand = new Random();
                        this.isEmergency = rand.nextInt(100) < 20; // 20% chance of emergency

                        if (isEmergency) {
                                System.out.println(Utils.threadInfo() + "[EMERGENCY - FUEL SHORTAGE] approaching with "
                                                + passengers + " passengers");
                                System.out.println(Utils.threadInfo()
                                                + "[EMERGENCY - FUEL SHORTAGE] Requesting permission to land");
                        } else {
                                System.out.println(
                                                Utils.threadInfo() + "Approaching with " + passengers + " passengers");
                                System.out.println(Utils.threadInfo() + "Requesting permission to land");
                        }
                        Gate assignedGate = airport.requestLanding(this);

                        long landingTime = System.currentTimeMillis();
                        long waitTime = landingTime - arrivalTime;

                        System.out.println(Utils.threadInfo() + "Received clearance! Landing on runway (waited " +
                                        waitTime + "ms)");

                        // Land on runway
                        Utils.sleep(800);
                        System.out.println(Utils.threadInfo() + "Landed successfully");

                        // Taxi to gate
                        System.out.println(Utils.threadInfo() + "Taxiing to Gate " + assignedGate.getId());
                        Utils.sleep(200);

                        // Dock at gate (gate was already reserved by ATC)
                        System.out.println(Utils.threadInfo() + "Docking at Gate " + assignedGate.getId());
                        Utils.sleep(500);
                        System.out.println(Utils.threadInfo() + "Docked at Gate " + assignedGate.getId()); // Release
                                                                                                           // runway
                        airport.releaseRunway();

                        // Ground operations (concurrent)
                        Thread disembarkThread = new Thread(() -> {
                                System.out.println(Utils.threadInfo() + "Passengers disembarking (" + passengers
                                                + " people)");
                                Utils.sleep(500);
                                System.out.println(Utils.threadInfo() + "All passengers disembarked");
                        }, "Disembark-P" + id);
                        Thread cleaningThread = new Thread(() -> {
                                System.out.println(Utils.threadInfo() + "Cleaning and resupplying");
                                Utils.sleep(500);
                                System.out.println(Utils.threadInfo() + "Cleaning complete");
                        }, "Cleaning-P" + id);
                        disembarkThread.start();
                        cleaningThread.start();

                        // Wait for concurrent operations
                        disembarkThread.join();
                        cleaningThread.join();

                        // Refuel (exclusive operation)
                        System.out.println(Utils.threadInfo() + "Requesting refuel truck");
                        airport.requestRefuel();

                        Thread refuelThread = new Thread(() -> {
                                System.out.println(Utils.threadInfo() + "Refueling...");
                                Utils.sleep(1000);
                                System.out.println(Utils.threadInfo() + "Refueling complete");
                        }, "Refuel-P" + id);
                        refuelThread.start();
                        refuelThread.join();
                        airport.releaseRefuel();

                        // Board passengers
                        Thread boardingThread = new Thread(() -> {
                                System.out.println(
                                                Utils.threadInfo() + "Boarding passengers (" + passengers + " people)");
                                Utils.sleep(500);
                                System.out.println(Utils.threadInfo() + "All passengers boarded");
                        }, "Boarding-P" + id);
                        boardingThread.start();
                        boardingThread.join();

                        // Undock
                        System.out.println(Utils.threadInfo() + "Undocking from Gate " + assignedGate.getId());
                        Utils.sleep(600);
                        assignedGate.setOccupied(false);

                        // Request runway for takeoff
                        System.out.println(Utils.threadInfo() + "Requesting runway for takeoff");
                        airport.requestRunwayForTakeoff(this);

                        // Taxi to runway
                        System.out.println(Utils.threadInfo() + "Taxiing to runway");
                        Utils.sleep(200);

                        // Takeoff
                        System.out.println(Utils.threadInfo() + "Taking off...");
                        Utils.sleep(500);
                        System.out.println(Utils.threadInfo() + "Departed successfully with " + passengers
                                        + " passengers");
                        airport.releaseRunway();
                        airport.recordDeparture(this, waitTime);

                } catch (InterruptedException e) {
                        System.err.println(Utils.threadInfo() + "Interrupted");
                        Thread.currentThread().interrupt();
                }
        }
}