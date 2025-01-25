package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.io.File;
import java.util.List;

/**
 * Utility class for verifying flight path constraints in tests.
 * Also has methods for checking file existence, etc.
 */
public class FlightPlannerUtils {

    /**
     * Represents a single move in flight:
     *  orderNo,
     *  fromLatitude,
     *  fromLongitude,
     *  angle,
     *  toLatitude,
     *  toLongitude
     *
     * Jackson needs either:
     *  - a no-arg constructor + public fields, or
     *  - @JsonCreator constructor with @JsonProperty fields
     *
     * Below we add a no-arg constructor plus keep public fields to allow field-based deserialization.
     */
    public static class FlightMove {
        // The JSON keys must match these exact field names:
        public String orderNo;
        public double fromLatitude;
        public double fromLongitude;
        public double angle;
        public double toLatitude;
        public double toLongitude;

        /**
         * A no-args constructor for Jackson to create a blank FlightMove object,
         * then populate fields from JSON.
         */
        public FlightMove() {
            // Jackson will fill the public fields afterwards
        }

        public FlightMove(String orderNo,
                          double fromLatitude,
                          double fromLongitude,
                          double angle,
                          double toLatitude,
                          double toLongitude) {
            this.orderNo = orderNo;
            this.fromLatitude = fromLatitude;
            this.fromLongitude = fromLongitude;
            this.angle = angle;
            this.toLatitude = toLatitude;
            this.toLongitude = toLongitude;
        }
    }


    public static class Cell {
        private final LngLat coordinates;

        public Cell(double lng, double lat) {
            coordinates = new LngLat(lng, lat);
        }

        public LngLat getCoordinates() {
            return coordinates;
        }
    }

    /**
     * Checks that each pair of consecutive moves in 'path' are "close"
     * (i.e. within SystemConstants.DRONE_IS_CLOSE_DISTANCE).
     */
    public static boolean allAdjacentPathMovesAreClose(List<Cell> path) {
        // If there's fewer than 2 cells, there's no adjacency to check
        if (path.size() < 2) {
            return true;
        }
        LngLatHandlingImpl handler = new LngLatHandlingImpl();
        for (int i = 1; i < path.size(); i++) {
            // Compare consecutive cells
            LngLat prev = path.get(i - 1).getCoordinates();
            LngLat curr = path.get(i).getCoordinates();
            if (!handler.isCloseTo(prev, curr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ensures that none of the cells in 'path' lie inside any no-fly zone.
     */
    public static boolean checkNoPathMoveIsInNoFlyZone(List<Cell> path, NamedRegion[] noFlyZones) {
        LngLatHandlingImpl handler = new LngLatHandlingImpl();
        for (Cell cell : path) {
            for (NamedRegion zone : noFlyZones) {
                if (handler.isInRegion(cell.getCoordinates(), zone)) {
                    return false; // found a point inside a no-fly zone
                }
            }
        }
        return true;
    }

    /**
     * Counts how many cells in 'path' are "close" to 'position'.
     * Returns true if exactly N cells are "close."
     */
    public static boolean checkCloseToPositionAppearsInPathNTimes(List<Cell> path,
                                                                  LngLat position,
                                                                  int N) {
        LngLatHandlingImpl handler = new LngLatHandlingImpl();
        int count = 0;
        for (Cell cell : path) {
            if (handler.isCloseTo(cell.getCoordinates(), position)) {
                count++;
            }
        }
        return (count == N);
    }

    /**
     * "Full" check that the path:
     *  1) is not empty,
     *  2) all adjacent moves are close,
     *  3) no cell is in no-fly zones,
     *  4) 'start' appears exactly twice,
     *  5) 'dest' appears exactly twice.
     */
    public static boolean checkValidPath(LngLat start,
                                         LngLat dest,
                                         List<Cell> path,
                                         NamedRegion[] noFlyZones) {
        if (path.isEmpty()) {
            return false;
        }
        if (!allAdjacentPathMovesAreClose(path)) {
            return false;
        }
        if (!checkNoPathMoveIsInNoFlyZone(path, noFlyZones)) {
            return false;
        }
        if (!checkCloseToPositionAppearsInPathNTimes(path, start, 2)) {
            return false;
        }
        if (!checkCloseToPositionAppearsInPathNTimes(path, dest, 2)) {
            return false;
        }
        return true;
    }

    /**
     * Utility method: checks if file at 'path' exists on disk.
     */
    public static boolean checkFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * Utility method: deletes a file at 'path' if it exists.
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}
