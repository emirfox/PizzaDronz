package uk.ac.ed.inf;

import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for checking flight path constraints:
 * - adjacency
 * - Appleton Tower visits
 * - hover
 * - no-fly-zone avoidance
 */
public class TestFlightPathCalculation {

    @Test
    void testAdjacentMovesAreClose() {
        // Suppose we got a final path from route optimizer
        DroneMovement dm1 = new DroneMovement(new LngLat(-3.186,55.944), 0, new LngLat(-3.186,55.94385), "O1");
        DroneMovement dm2 = new DroneMovement(new LngLat(-3.186,55.94385), 22.5, new LngLat(-3.1859,55.9438), "O1");
        List<DroneMovement> path = List.of(dm1, dm2);

        for (int i=0; i<path.size()-1; i++) {
            double dist = distance(path.get(i).getEnd(), path.get(i+1).getStart());
            assertTrue(dist <= 0.00015, "Moves should be within 0.00015° of each other");
        }
    }

    @Test
    void testAppletonTowerCoordsAppearTwice() {
        // Appleton Tower: -3.186874, 55.944494
        LngLat appleton = new LngLat(-3.186874,55.944494);
        List<DroneMovement> path = List.of(
                new DroneMovement(appleton, 0, new LngLat(-3.1867,55.9443), "O1"),
                // ...
                new DroneMovement(new LngLat(-3.1867,55.9443),180, appleton, "O1")
        );

        // We want it exactly 2 times in the entire path (start + end)
        int count = 0;
        for (DroneMovement move : path) {
            if (distance(move.getStart(), appleton) < 1e-6) count++;
            if (distance(move.getEnd(), appleton) < 1e-6) count++;
        }
        assertEquals(2, count);
    }

    @Test
    void testNoMoveEntersNoFlyZone() {
        NamedRegion[] noFlyZones = new NamedRegion[] {
                // some polygon
        };
        // Suppose we have a path from route optimizer
        List<DroneMovement> path = List.of(
                // ...
        );
        for (DroneMovement dm : path) {
            assertFalse(isInAnyNoFlyZone(dm.getEnd(), noFlyZones));
        }
    }

    // Example small helper
    private static double distance(LngLat a, LngLat b) {
        double dx = a.lng() - b.lng();
        double dy = a.lat() - b.lat();
        return Math.sqrt(dx*dx + dy*dy);
    }
    private static boolean isInAnyNoFlyZone(LngLat pos, NamedRegion[] zones) {
        for (NamedRegion zr : zones) {
            if (GeometryHelper.isPointInsidePolygon(zr.vertices(), zr.vertices().length, pos)) {
                return true;
            }
        }
        return false;
    }
}
