package uk.ac.ed.inf;

import junit.framework.TestCase;
import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

public class TestLngLatHandlingImpl extends TestCase {

    private LngLatHandlingImpl handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        handler = new LngLatHandlingImpl();
    }

    public void testDistanceTo() {
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(3.0, 4.0);
        double dist = handler.distanceTo(p1, p2);
        assertEquals(5.0, dist, 1e-9);
    }

    public void testIsCloseTo() {
        LngLat p1 = new LngLat(0.0, 0.0);
        LngLat p2 = new LngLat(0.0, SystemConstants.DRONE_IS_CLOSE_DISTANCE * 0.9999);
        // p2 is just within the threshold
        assertTrue(handler.isCloseTo(p1, p2));

        LngLat p3 = new LngLat(0.0, SystemConstants.DRONE_IS_CLOSE_DISTANCE * 1.1);
        // p3 is slightly beyond threshold
        assertFalse(handler.isCloseTo(p1, p3));
    }

    public void testNextPositionValidAngle() {
        LngLat start = new LngLat(0.0, 0.0);
        // angle 0 => move East by DRONE_MOVE_DISTANCE
        LngLat next = handler.nextPosition(start, 0.0);
        assertEquals(SystemConstants.DRONE_MOVE_DISTANCE, next.lng(), 1e-9);
        assertEquals(0.0, next.lat(), 1e-9);

        // angle 90 => move North by DRONE_MOVE_DISTANCE
        LngLat nextN = handler.nextPosition(start, 90.0);
        assertEquals(0.0, nextN.lng(), 1e-9);
        assertEquals(SystemConstants.DRONE_MOVE_DISTANCE, nextN.lat(), 1e-9);
    }

    public void testNextPositionInvalidAngle() {
        LngLat start = new LngLat(0.0, 0.0);
        try {
            handler.nextPosition(start, 15.0); // 15 not a multiple of 22.5 => should throw
            fail("Expected an IllegalArgumentException for angle not multiple of 22.5");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    public void testIsInRegion() {
        // Suppose we define a simple rectangular region from (0,0)->(2,0)->(2,2)->(0,2)
        NamedRegion region = new NamedRegion(
                "TestPoly",
                new LngLat[]{
                        new LngLat(0.0,0.0),
                        new LngLat(2.0,0.0),
                        new LngLat(2.0,2.0),
                        new LngLat(0.0,2.0)
                }
        );

        // Inside
        LngLat inside = new LngLat(1.0,1.0);
        assertTrue(handler.isInRegion(inside, region));

        // Outside
        LngLat outside = new LngLat(3.0,3.0);
        assertFalse(handler.isInRegion(outside, region));
    }
}
