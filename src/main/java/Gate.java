public class Gate {
    private final int id;
    private volatile boolean occupied;
    
    public Gate(int id) {
        this.id = id;
        this.occupied = false;
    }
    
    public int getId() {
        return id;
    }
    
    public synchronized boolean isOccupied() {
        return occupied;
    }
    
    public synchronized void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}