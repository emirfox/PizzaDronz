package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

/**
 * Utility class for geometric calculations, especially for determining if a point is inside a polygon.
 * It provides methods for checking if a point lies on a line, the orientation of three points, and if lines intersect.
 */
public class GeometryHelper {
    private final LngLat p1; // First point of the line
    private final LngLat p2; // Second point of the line

    /**
     * Constructor for GeometryHelper.
     * @param p1 The first point of the line.
     * @param p2 The second point of the line.
     */
    public GeometryHelper(LngLat p1, LngLat p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Checks if a given point lies on a line.
     * @param line The line to check against.
     * @param point The point to check.
     * @return True if the point lies on the line, false otherwise.
     */
    private static boolean isOnLine(GeometryHelper line, LngLat point) {
        return point.lng() <= Math.max(line.p1.lng(), line.p2.lng()) &&
                point.lng() >= Math.min(line.p1.lng(), line.p2.lng()) &&
                point.lat() <= Math.max(line.p1.lat(), line.p2.lat()) &&
                point.lat() >= Math.min(line.p1.lat(), line.p2.lat());
    }

    /**
     * Determines the orientation of three points (clockwise, counterclockwise, collinear).
     * @param a First point.
     * @param b Second point.
     * @param c Third point.
     * @return 0 if collinear, 1 if clockwise, 2 if counterclockwise.
     */
    private static int calculateDirection(LngLat a, LngLat b, LngLat c) {
        double value = (b.lat() - a.lat()) * (c.lng() - b.lng()) - (b.lng() - a.lng()) * (c.lat() - b.lat());
        if (value == 0) return 0; // Collinear
        return (value < 0) ? 2 : 1; // Anti-clockwise or Clockwise
    }

    /**
     * Checks if two lines intersect.
     * @param l1 First line.
     * @param l2 Second line.
     * @return True if the lines intersect, false otherwise.
     */
    private static boolean doLinesIntersect(GeometryHelper l1, GeometryHelper l2) {
        int dir1 = calculateDirection(l1.p1, l1.p2, l2.p1);
        int dir2 = calculateDirection(l1.p1, l1.p2, l2.p2);
        int dir3 = calculateDirection(l2.p1, l2.p2, l1.p1);
        int dir4 = calculateDirection(l2.p1, l2.p2, l1.p2);

        if (dir1 != dir2 && dir3 != dir4) return true; // General case
        // Special Cases
        if (dir1 == 0 && isOnLine(l1, l2.p1)) return true;
        if (dir2 == 0 && isOnLine(l1, l2.p2)) return true;
        if (dir3 == 0 && isOnLine(l2, l1.p1)) return true;
        if (dir4 == 0 && isOnLine(l2, l1.p2)) return true;

        return false;
    }

    /**
     * Determines if a point lies inside a polygon using ray casting algorithm.
     * @param polygonVertices Array of points that form the polygon vertices.
     * @param vertexCount The number of vertices in the polygon.
     * @param point The point to check.
     * @return True if the point is inside the polygon, false otherwise.
     */
    public static boolean isPointInsidePolygon(LngLat[] polygonVertices, int vertexCount, LngLat point) {
        if (vertexCount < 3) return false; // Not a polygon

        // Extending point to infinity
        LngLat extendingPoint = new LngLat(999.99, point.lat());
        GeometryHelper extendingLine = new GeometryHelper(point, extendingPoint);
        int intersectionCount = 0;

        // Count intersections of the extended line with sides of polygon
        for (int i = 0; i < vertexCount; i++) {
            GeometryHelper side = new GeometryHelper(polygonVertices[i], polygonVertices[(i + 1) % vertexCount]);
            if (doLinesIntersect(side, extendingLine)) {
                // Point on polygon edge
                if (calculateDirection(side.p1, point, side.p2) == 0) {
                    return isOnLine(side, point);
                }
                intersectionCount++;
            }
        }
        // Odd number of intersections means inside, even means outside
        return (intersectionCount % 2 == 1);
    }
}
