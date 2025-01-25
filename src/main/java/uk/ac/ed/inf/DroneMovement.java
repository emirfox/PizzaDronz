package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

/**
 * Immutable class representing the movement of a drone. It includes starting and ending coordinates,
 * the angle of movement, and the associated order number.
 */
public final class DroneMovement {
    private final LngLat start;
    private final LngLat end;
    private final double angle;
    private final String orderNo;

    /**
     * Constructs a new DroneMovement.
     *
     * @param start    Starting coordinates of the move.
     * @param angle    Angle of movement in degrees.
     * @param end      Ending coordinates of the move.
     * @param orderNo  Associated order number.
     */
    public DroneMovement(LngLat start, double angle, LngLat end, String orderNo) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end coordinates cannot be null.");
        }
        this.start = start;
        this.angle = angle;
        this.end = end;
        this.orderNo = orderNo;
    }

    public LngLat getStart() {
        return start;
    }

    public double getAngle() {
        return angle;
    }

    public LngLat getEnd() {
        return end;
    }

    public String getOrderNo() {
        return orderNo;
    }
}