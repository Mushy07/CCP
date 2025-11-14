import java.util.concurrent.*;
import java.util.LinkedList;
import java.util.Queue;

public class Airport implements Runnable {
    private final Gate[] gates;
    private final Semaphore runwayAccess;
    private final Semaphore refuelTruck;
    private final Semaphore airportCapacity;
    private final Queue<Plane> landingQueue;
    private final Queue<Plane> emergencyQueue;
    private final Queue<ATCRequest> requestQueue;
    private final Object queueLock;
    private volatile boolean running;
    private final Statistics stats;
    private volatile Integer runwayOccupiedBy; // Track which plane is using the runway

    // Request types for ATC
    private enum RequestType {
        LANDING_REQUEST,
        RUNWAY_RELEASE,
        TAKEOFF_REQUEST,
        REFUEL_REQUEST,
        REFUEL_RELEASE,
        DEPARTURE_RECORD
    }

    // ATC Request wrapper
    private static class ATCRequest {
        final RequestType type;
        final Plane plane;
        final CountDownLatch responseLatch;
        Gate assignedGate;
        Long waitTime;
        boolean granted; // Whether the request was granted or denied

        ATCRequest(RequestType type, Plane plane) {
            this.type = type;
            this.plane = plane;
            this.responseLatch = new CountDownLatch(1);
            this.granted = true; // Default to granted
        }
    }

    public Airport(int numGates) {
        this.gates = new Gate[numGates];
        for (int i = 0; i < numGates; i++) {
            gates[i] = new Gate(i + 1);
        }

        this.runwayAccess = new Semaphore(1);
        this.refuelTruck = new Semaphore(1);
        this.airportCapacity = new Semaphore(3); // Max 3 planes on ground
        this.landingQueue = new LinkedList<>();
        this.emergencyQueue = new LinkedList<>();
        this.requestQueue = new LinkedList<>();
        this.queueLock = new Object();
        this.running = true;
        this.stats = new Statistics();
        this.runwayOccupiedBy = null;
    }

    @Override
    public void run() {
        System.out.println(Utils.threadInfo() + "Air Traffic Control online");

        while (running || !requestQueue.isEmpty()) {
            ATCRequest request = null;

            synchronized (queueLock) {
                if (!requestQueue.isEmpty()) {
                    request = requestQueue.poll();
                }
            }

            if (request != null) {
                processRequest(request);
            } else {
                try {
                    Thread.sleep(100); // ATC monitoring interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println(Utils.threadInfo() + "Air Traffic Control shutting down");
    }

    private void processRequest(ATCRequest request) {
        try {
            switch (request.type) {
                case LANDING_REQUEST:
                    handleLandingRequest(request);
                    break;
                case RUNWAY_RELEASE:
                    handleRunwayRelease(request);
                    break;
                case TAKEOFF_REQUEST:
                    handleTakeoffRequest(request);
                    break;
                case REFUEL_REQUEST:
                    handleRefuelRequest(request);
                    break;
                case REFUEL_RELEASE:
                    handleRefuelRelease(request);
                    break;
                case DEPARTURE_RECORD:
                    handleDepartureRecord(request);
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(Utils.threadInfo() + "ATC request processing interrupted");
        }
    }

    private void handleLandingRequest(ATCRequest request) throws InterruptedException {
        Plane plane = request.plane;

        // Only add to queue if this is the first request (not a retry)
        boolean isInQueue = landingQueue.contains(plane) || emergencyQueue.contains(plane);

        if (!isInQueue) {
            if (plane.isEmergency()) {
                emergencyQueue.add(plane);
                System.out.println(Utils.threadInfo() + "EMERGENCY! Plane " +
                        plane.getId() + " added to priority queue");
            } else {
                landingQueue.add(plane);
                System.out.println(Utils.threadInfo() + "Plane " + plane.getId() +
                        " added to landing queue (position: " + landingQueue.size() + ")");
            }
        }

        // Enforce queue order - emergency planes have priority, then FIFO order
        Plane nextInLine = null;
        if (!emergencyQueue.isEmpty()) {
            nextInLine = emergencyQueue.peek();
        } else if (!landingQueue.isEmpty()) {
            nextInLine = landingQueue.peek();
        }

        // If this plane is not next in line, deny and let it retry (silently)
        if (nextInLine != plane) {
            request.granted = false;
            request.responseLatch.countDown();
            return;
        }

        // Check airport capacity first (non-blocking)
        if (!airportCapacity.tryAcquire()) {
            System.out.println(Utils.threadInfo() + "Airport at capacity - Plane " +
                    plane.getId() + " holding in airspace");
            request.granted = false;
            request.responseLatch.countDown();
            return;
        }

        // Check runway availability (non-blocking)
        if (!runwayAccess.tryAcquire()) {
            airportCapacity.release();
            String occupiedMsg = (runwayOccupiedBy != null)
                    ? " (occupied by Plane " + runwayOccupiedBy + ")"
                    : "";
            System.out.println(Utils.threadInfo() + "Runway busy" + occupiedMsg + " - Plane " +
                    plane.getId() + " holding in airspace");
            request.granted = false;
            request.responseLatch.countDown();
            return;
        }

        // Try to assign gate with atomic check-and-reserve (synchronized)
        Gate assignedGate;
        synchronized (gates) {
            assignedGate = tryAssignGate(plane);
            if (assignedGate == null) {
                // No gate available, release resources
                runwayAccess.release();
                airportCapacity.release();
                System.out.println(Utils.threadInfo() + "No suitable gate available - Plane " +
                        plane.getId() + " holding in airspace");
                request.granted = false;
                request.responseLatch.countDown();
                return;
            }
            // Reserve the gate immediately (atomic with check)
            assignedGate.setOccupied(true);
        }

        runwayOccupiedBy = plane.getId();
        System.out.println(Utils.threadInfo() + "Gate " + assignedGate.getId() +
                " reserved for Plane " + plane.getId());

        // Remove from queue
        if (plane.isEmergency()) {
            emergencyQueue.remove(plane);
        } else {
            landingQueue.remove(plane);
        }

        System.out.println(Utils.threadInfo() + "Cleared Plane " + plane.getId() +
                " to land at Gate " + assignedGate.getId());

        request.assignedGate = assignedGate;
        request.granted = true;
        request.responseLatch.countDown();
    }

    private void handleRunwayRelease(ATCRequest request) {
        runwayAccess.release();
        runwayOccupiedBy = null;
        System.out.println(Utils.threadInfo() + "Runway released");
        request.responseLatch.countDown();
    }

    private void handleTakeoffRequest(ATCRequest request) throws InterruptedException {
        if (runwayAccess.tryAcquire()) {
            runwayOccupiedBy = request.plane.getId();
            System.out.println(Utils.threadInfo() + "Runway cleared for takeoff");
            request.granted = true;
        } else {
            // Runway not available, plane must wait
            String occupiedMsg = (runwayOccupiedBy != null)
                    ? " (occupied by Plane " + runwayOccupiedBy + ")"
                    : "";
            System.out.println(Utils.threadInfo() + "Runway busy" + occupiedMsg + " - Plane must wait for takeoff");
            request.granted = false;
        }
        request.responseLatch.countDown();
    }

    private void handleRefuelRequest(ATCRequest request) throws InterruptedException {
        if (refuelTruck.tryAcquire()) {
            System.out.println(Utils.threadInfo() + "Refuel truck assigned");
            request.granted = true;
        } else {
            // Refuel truck busy, plane must wait
            System.out.println(Utils.threadInfo() + "Refuel truck busy - Plane must wait");
            request.granted = false;
        }
        request.responseLatch.countDown();
    }

    private void handleRefuelRelease(ATCRequest request) {
        refuelTruck.release();
        System.out.println(Utils.threadInfo() + "Refuel truck available");
        request.responseLatch.countDown();
    }

    private void handleDepartureRecord(ATCRequest request) {
        airportCapacity.release();
        stats.recordPlane(request.plane, request.waitTime);
        System.out.println(Utils.threadInfo() + "Plane " + request.plane.getId() +
                " departed. Airport capacity released.");
        request.responseLatch.countDown();
    }

    public Gate requestLanding(Plane plane) throws InterruptedException {
        while (true) {
            ATCRequest request = new ATCRequest(RequestType.LANDING_REQUEST, plane);

            synchronized (queueLock) {
                requestQueue.add(request);
            }

            // Wait for ATC to process the request
            request.responseLatch.await();

            if (request.granted && request.assignedGate != null) {
                return request.assignedGate;
            }

            // Request denied, plane circles in airspace before retrying
            System.out.println(Utils.threadInfo() + "Circling in airspace, waiting for clearance...");
            Thread.sleep(1000); // Wait 1 second before retrying to reduce spam
        }
    }

    // Must be called within synchronized(gates) block
    private Gate tryAssignGate(Plane plane) {
        if (plane.isEmergency()) {
            // Emergency planes prefer gate 3, but will use any available gate
            // First priority: Emergency gate (Gate 3)
            Gate emergencyGate = gates[gates.length - 1];
            if (!emergencyGate.isOccupied()) {
                System.out.println(Utils.threadInfo() + "Assigned emergency Plane " +
                        plane.getId() + " to Gate 3 (emergency gate)");
                return emergencyGate;
            }

            // Second priority: Any available gate (Gates 1-2)
            for (int i = 0; i < gates.length - 1; i++) {
                if (!gates[i].isOccupied()) {
                    System.out.println(Utils.threadInfo() + "Emergency gate occupied! " +
                            "Assigned emergency Plane " + plane.getId() +
                            " to Gate " + gates[i].getId() + " (alternate)");
                    return gates[i];
                }
            }

            // All gates occupied
            System.out.println(Utils.threadInfo() + "CRITICAL - All gates occupied, emergency Plane " +
                    plane.getId() + " must wait!");
            return null;
        }

        // Normal planes use gates 1 and 2 only
        for (int i = 0; i < gates.length - 1; i++) {
            if (!gates[i].isOccupied()) {
                System.out.println(Utils.threadInfo() + "Assigned normal Plane " +
                        plane.getId() + " to Gate " + gates[i].getId());
                return gates[i];
            }
        }

        // Normal planes cannot use emergency gate - Gate 3 is for emergencies only
        System.out.println(Utils.threadInfo() + "Gates 1-2 occupied, normal Plane " +
                plane.getId() + " must wait");
        return null;
    }

    public void releaseRunway() throws InterruptedException {
        ATCRequest request = new ATCRequest(RequestType.RUNWAY_RELEASE, null);

        synchronized (queueLock) {
            requestQueue.add(request);
        }

        request.responseLatch.await();
    }

    public void requestRunwayForTakeoff(Plane plane) throws InterruptedException {
        while (true) {
            ATCRequest request = new ATCRequest(RequestType.TAKEOFF_REQUEST, plane);

            synchronized (queueLock) {
                requestQueue.add(request);
            }

            request.responseLatch.await();

            if (request.granted) {
                return; // Runway granted
            }

            // Runway busy, wait before retrying
            Thread.sleep(1000);
        }
    }

    public void requestRefuel() throws InterruptedException {
        while (true) {
            ATCRequest request = new ATCRequest(RequestType.REFUEL_REQUEST, null);

            synchronized (queueLock) {
                requestQueue.add(request);
            }

            request.responseLatch.await();

            if (request.granted) {
                return; // Refuel truck granted
            }

            // Truck busy, wait before retrying
            Thread.sleep(1000);
        }
    }

    public void releaseRefuel() throws InterruptedException {
        ATCRequest request = new ATCRequest(RequestType.REFUEL_RELEASE, null);

        synchronized (queueLock) {
            requestQueue.add(request);
        }

        request.responseLatch.await();
    }

    public void recordDeparture(Plane plane, long waitTime) throws InterruptedException {
        ATCRequest request = new ATCRequest(RequestType.DEPARTURE_RECORD, plane);
        request.waitTime = waitTime;

        synchronized (queueLock) {
            requestQueue.add(request);
        }

        request.responseLatch.await();
    }

    public void shutdown() {
        running = false;
    }

    public void printStatistics() {
        System.out.println("\n========== SANITY CHECKS ==========");
        boolean allGatesEmpty = true;
        for (Gate gate : gates) {
            String status = gate.isOccupied() ? "OCCUPIED" : "EMPTY";
            System.out.println("Gate " + gate.getId() + ": " + status);
            if (gate.isOccupied()) {
                allGatesEmpty = false;
            }
        }

        System.out.println("All gates empty: " + (allGatesEmpty ? "PASS" : "FAIL"));

        System.out.println("\n========== STATISTICS ==========");
        stats.printStatistics();
    }
}