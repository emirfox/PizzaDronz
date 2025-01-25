package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;
import uk.ac.ed.inf.ilp.constant.SystemConstants;

/**
 * Implementation of the LngLatHandling interface, providing methods to handle Longitude and Latitude calculations.
 */
public class LngLatHandlingImpl implements LngLatHandling {

    /**
     * Calculates the Euclidean distance between two positions.
     * @param startPosition The starting position (LngLat object).
     * @param endPosition The end position (LngLat object).
     * @return The Euclidean distance between the two positions.
     */
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        double xDifference = endPosition.lng() - startPosition.lng();
        double yDifference = endPosition.lat() - startPosition.lat();
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference);
    }

    /**
     * Determines if one position is close to another position, within a defined distance threshold.
     * @param startPosition The starting position (LngLat object).
     * @param otherPosition The position to compare with (LngLat object).
     * @return True if the positions are within the threshold distance, false otherwise.
     */
    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) <= SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    /**
     * Checks if a given position is inside a specified region.
     * @param position The position to check (LngLat object).
     * @param region The region to check against (NamedRegion object).
     * @return True if the position is inside the region, false otherwise.
     */
    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        LngLat[] vertices = region.vertices();
        int numberOfVertices = vertices.length;
        return GeometryHelper.isPointInsidePolygon(vertices, numberOfVertices, position);
    }

    /**
     * Calculates the next position based on a starting position and an angle of movement.
     * @param startPosition The starting position (LngLat object).
     * @param angle The angle of movement in degrees.
     * @return The new position (LngLat object) after moving in the specified direction.
     */
    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        if (angle == 999) {
            return startPosition;
        }
        if (angle < 0 || angle > 360 || angle % 22.5 != 0) {
            throw new IllegalArgumentException("Angle must be between 0 and 360 and a multiple of 22.5");
        }
        double radianAngle = Math.toRadians(angle);
        double newLongitude = startPosition.lng() + SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(radianAngle);
        double newLatitude = startPosition.lat() + SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(radianAngle);
        return new LngLat(newLongitude, newLatitude);
    }
}
