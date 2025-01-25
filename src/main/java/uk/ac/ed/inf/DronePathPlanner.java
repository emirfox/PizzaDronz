package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;

/**
 * Class responsible for planning the flight path of a drone.
 * This includes calculating paths while avoiding no-fly zones and ensuring the drone remains within a central area.
 */
public class DronePathPlanner {
    private final NamedRegion centralArea; // The central area within which the drone must operate.
    private final NamedRegion[] noFlyZones; // Array of regions where the drone is not allowed to fly.
    private final HashMap<String, List<DroneMovement>> cachedPaths = new HashMap<>(); // Cache to store computed paths for efficiency.
    private final LngLatHandlingImpl lngLatHandler = new LngLatHandlingImpl(); // Handler for operations related to longitude and latitude.

    /**
     * Constructor for DronePathPlanner class.
     * @param noFlyZones Array of no-fly zones to avoid during path planning.
     * @param centralArea The central operational area for the drone.
     */
    public DronePathPlanner(NamedRegion[] noFlyZones, NamedRegion centralArea) {
        this.noFlyZones = noFlyZones;
        this.centralArea = centralArea;
    }

    /**
     * Calculates the path from a start to an end location considering no-fly zones and central area constraints.
     * @param start The starting point of the path.
     * @param end The destination point of the path.
     * @param orderNo The order number associated with the movement, for tracking purposes.
     * @return A list of drone movements forming a path from the start to the end location.
     */
    private List<DroneMovement> calculatePath(LngLat start, LngLat end, String orderNo) {
        List<LngLat> previousPositions = new ArrayList<>(); // Tracks previously visited positions to avoid loops.
        List<DroneMovement> path = new ArrayList<>(); // List to store the path as a sequence of movements.
        LngLat currentPosition = start; // Current position of the drone, starting at the start location.

        // Defines possible angles for drone movement.
        double[] angles = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5};

        // Continuously calculate the next move until the drone is close to the destination.
        while (!lngLatHandler.isCloseTo(currentPosition, end)) {
            double closestDistance = Double.MAX_VALUE; // Initialize closest distance to a large value.
            double chosenAngle = 0; // Angle for the next move.

            // Evaluate each possible angle to determine the best next move.
            for (double angle : angles) {
                LngLat nextPosition = lngLatHandler.nextPosition(currentPosition, angle); // Calculate next position for the given angle.
                // Check if the next position is valid and has not been previously visited.
                if (!previousPositions.contains(nextPosition) && isValidMove(currentPosition, nextPosition)) {
                    double distance = lngLatHandler.distanceTo(nextPosition, end); // Calculate distance to the end location.
                    // Update the closest distance and chosen angle if this move is better.
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        chosenAngle = angle;
                    }
                }
            }

            // Add the chosen move to the path.
            DroneMovement movement = new DroneMovement(currentPosition, chosenAngle, lngLatHandler.nextPosition(currentPosition, chosenAngle), orderNo);
            path.add(movement);

            // Update the current position to the end of the chosen move.
            currentPosition = lngLatHandler.nextPosition(currentPosition, chosenAngle);
            previousPositions.add(currentPosition); // Add the current position to the list of visited positions.
        }

        // Add a hover move at the end location.
        path.add(new DroneMovement(currentPosition, 999, currentPosition, orderNo));
        return path;
    }

    /**
     * Checks if a proposed move from a current position to a next position is valid considering no-fly zones and central area constraints.
     * @param currentPos The current position of the drone.
     * @param nextPos The proposed next position of the drone.
     * @return True if the move is valid, otherwise False.
     */
    private boolean isValidMove(LngLat currentPos, LngLat nextPos) {
        boolean currentlyInCentral = lngLatHandler.isInCentralArea(currentPos, centralArea); // Check if the current position is in the central area.
        boolean nextInCentral = lngLatHandler.isInCentralArea(nextPos, centralArea); // Check if the next position is in the central area.
        for (NamedRegion noFlyZone : noFlyZones) {
            if (lngLatHandler.isInRegion(nextPos, noFlyZone)) {
                return false; // Move is invalid if the next position is in a no-fly zone.
            }
        }
        return currentlyInCentral || !nextInCentral; // Move is valid if it either stays in the central area or does not re-enter it.
    }

    /**
     * Finds the total path for a round trip from a start to an end location and back again.
     * Utilizes caching to avoid recalculating paths for identical start and end points.
     * @param start The starting point of the path.
     * @param end The destination point of the path.
     * @param orderNo The order number associated with the movement.
     * @return A list of drone movements forming a complete round trip path.
     */
    public List<DroneMovement> findTotalPath(LngLat start, LngLat end, String orderNo) {
        String key = "KEY:" + start.lng() + start.lat() + end.lng() + end.lat(); // Cache key based on start and end coordinates.
        if (cachedPaths.containsKey(key)) {
            return copyCachedPath(cachedPaths.get(key), orderNo); // Use cached path if available.
        } else {
            // Calculate new path if not cached.
            List<DroneMovement> pathToDestination = calculatePath(start, end, orderNo);
            List<DroneMovement> returnPath = reversePath(pathToDestination); // Calculate the return path.
            pathToDestination.addAll(returnPath); // Combine paths for the complete round trip.
            cachedPaths.put(key, pathToDestination); // Cache the new path.
            return pathToDestination;
        }
    }

    /**
     * Copies a path from the cache, updating the order number for each movement.
     * This allows reusing paths for different orders.
     * @param originalPath The original path from the cache.
     * @param newOrderNo The new order number to be assigned to the movements.
     * @return A list of drone movements with the updated order number.
     */
    private List<DroneMovement> copyCachedPath(List<DroneMovement> originalPath, String newOrderNo) {
        List<DroneMovement> newPath = new ArrayList<>();
        for (DroneMovement movement : originalPath) {
            newPath.add(new DroneMovement(movement.getStart(), movement.getAngle(), movement.getEnd(), newOrderNo));
        }
        return newPath;
    }

    /**
     * Reverses a path to create a return journey.
     * This method is used to calculate the path for the drone to return to its starting point.
     * @param path The original path to be reversed.
     * @return A list of drone movements forming the return path.
     */
    private List<DroneMovement> reversePath(List<DroneMovement> path) {
        List<DroneMovement> reversedPath = new ArrayList<>();
        for (int i = path.size() - 2; i >= 0; i--) { // Start from second to last to avoid adding hover move twice
            DroneMovement movement = path.get(i);
            reversedPath.add(new DroneMovement(movement.getEnd(), (movement.getAngle() + 180) % 360, movement.getStart(), movement.getOrderNo()));
        }
        return reversedPath;
    }
}
