# Asia Pacific Airport Simulation

A Java-based concurrent simulation of airport operations featuring Air Traffic Control (ATC), aircraft management, and resource coordination.

## Project Overview

This simulation models a small airport with specific constraints and demonstrates concurrent programming concepts using Java threads and synchronization primitives.

## Requirements Implemented

### Basic Requirements ✅
- **Single Runway**: Only 1 runway for all landing and takeoff operations
- **Airport Capacity**: Maximum 3 aircraft on airport grounds (including runway and gates)
- **Aircraft Lifecycle**: Complete sequence of land → taxi to gate → dock → disembark → refuel → restock → board → taxi to runway → takeoff
- **Realistic Timing**: Each operation takes appropriate time with random variations
- **No Ground Waiting**: Aircraft cannot wait on ground for gates (must circle/queue for landing permission)

### Concurrency Features ✅
- **Concurrent Operations**: Passenger disembarkation and aircraft cleaning happen simultaneously
- **Exclusive Refueling**: Only 1 refuel truck available (exclusive access required)
- **Thread Safety**: All shared resources protected with proper synchronization

### Congestion Scenario ✅
- **6 Aircraft Total**: Simulates realistic airport traffic
- **Emergency Handling**: Final aircraft arrives with low fuel requiring emergency landing
- **Resource Contention**: 2 gates for 6 planes creates natural congestion
- **Priority Processing**: Emergency aircraft get landing priority

### Statistics & Validation ✅
- **Sanity Checks**: Verifies all gates are empty at simulation end
- **Comprehensive Stats**: Min/Max/Average waiting times, planes served, passengers boarded
- **Thread Identification**: All output shows which thread is performing each action

## Key Design Assumptions

1. **Airport Layout**: 2 gates + 1 runway = 3 total spots (airport capacity constraint)
2. **Passenger Capacity**: Each aircraft carries 20-50 passengers (randomly generated)
3. **Fuel Levels**: Normal planes have 80-100% fuel, emergency plane has 5% fuel
4. **Timing Constraints**: All operations complete within 60 seconds simulation window
5. **Emergency Protocol**: Low fuel aircraft get immediate priority and can force gate evacuation
6. **Thread Safety**: Each plane, ATC, passengers, and ground crew operate as separate threads

## Project Structure

```
CCP/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
└── src/main/java/
    ├── Main.java                     # Main class and entry point
    ├── Airport.java                  # Airport and ATC management
    ├── Plane.java                    # Aircraft thread and lifecycle
    ├── Statistics.java               # Data collection and reporting
    ├── Util.java                     # Utility classes (LandingRequest)
    ├── Gate.java                     # Gate management functionality
    └── Config.java                   # Configuration constants
```

## How to Run

### Prerequisites
- Java JDK 11+ installed
- Maven installed (or use VS Code's integrated Maven support)

### Compilation & Execution

#### Option 1: Using Maven (Recommended)
```powershell
# Compile the project
mvn clean compile

# Run the simulation
mvn exec:java -Dexec.mainClass="Main"
```

#### Option 2: Using javac directly
```powershell
# Compile
javac -d target/classes src/main/java/*.java

# Run
java -cp target/classes Main
```

#### Option 3: Using VS Code
1. Open the project folder in VS Code
2. Navigate to `src/main/java/Main.java`
3. Click the "Run" button above the `main` method
4. Or press `F5` to debug

## Sample Output

The simulation produces detailed output showing:
- Thread names for all operations (ensuring no thread acts for another)
- Aircraft arrival and landing requests
- ATC decision making and resource allocation
- Emergency handling and priority processing
- Concurrent ground operations
- Resource release and cleanup
- Final sanity checks and statistics

Example output snippet:
```
MainThread: Starting Asia Pacific Airport Simulation
Plane-1-Thread: Approaching airport (Fuel: 85%, Passengers: 42)
ATC-Thread: Processing landing request from Plane-1-Thread
Passenger-Disembark-1: Passengers disembarking from Plane-1-Thread
Cleaning-Crew-1: Cleaning and restocking Plane-1-Thread
Plane-6-Thread: Requesting landing permission (EMERGENCY)
ATC-Thread: EMERGENCY! Forcing gate evacuation for Plane-6-Thread
...
ATC: ✓ SANITY CHECK PASSED - Airport is properly empty
Statistics: Planes served: 6
Statistics: Total passengers boarded: 234
Statistics: Waiting times (ms): Min: 127ms, Avg: 2,456ms, Max: 8,923ms
```

## Technical Implementation

### Core Classes
- **`Main`**: Application entry point and simulation orchestration
- **`Airport`**: Airport resources and ATC management
- **`Plane`**: Individual aircraft thread and lifecycle management
- **`Statistics`**: Data collection and reporting
- **`Util`**: Utility classes including LandingRequest
- **`Gate`**: Gate management functionality
- **`Config`**: Configuration constants and settings

### Synchronization Mechanisms
- **`Semaphore airportCapacity`**: Controls total aircraft on grounds (capacity: 3)
- **`Semaphore gateCapacity`**: Manages gate availability (capacity: 2)
- **`Semaphore runway`**: Ensures exclusive runway access (capacity: 1)
- **`Semaphore refuelTruck`**: Single refuel truck coordination (capacity: 1)

### Thread Safety Features
- Atomic counters for statistics
- Synchronized collections for shared data
- Proper resource acquisition/release patterns
- CompletableFuture for concurrent ground operations

## Assignment Compliance

✅ **Concurrency**: Uses proper Java threading with semaphores and thread-safe collections  
✅ **Airport Constraints**: Enforces 1 runway, 3-aircraft capacity, 2 gates  
✅ **Realistic Operations**: Complete aircraft lifecycle with appropriate timing  
✅ **Emergency Handling**: Low-fuel aircraft get priority and force gate evacuation  
✅ **Statistics**: Comprehensive reporting with sanity checks  
✅ **Thread Identification**: All output clearly shows executing thread  
✅ **No Cross-Thread Actions**: Each thread only acts for itself  
✅ **60-Second Limit**: Simulation completes within time constraint

## Troubleshooting

If you encounter issues:

1. **Java Version**: Ensure you're using JDK 11 or higher
2. **Maven Issues**: Run `mvn clean` before compiling
3. **Thread Deadlocks**: The simulation includes timeout mechanisms to prevent hanging
4. **Output Clarity**: Each line shows the executing thread name for debugging

## License

This project is created for educational purposes as part of a concurrent programming assignment.