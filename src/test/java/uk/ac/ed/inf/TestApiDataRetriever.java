package uk.ac.ed.inf;

import com.sun.net.httpserver.HttpContext;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.IOException;

/**
 * JUnit 4 test class to verify ApiDataRetriever using a local MockRestServer.
 */
public class TestApiDataRetriever extends TestCase {

    private MockRestServer mockServer;
    private final int port = 8005; // or any free port

    @Before
    public void setUp() throws IOException {
        // 1) Start mock server for each test
        mockServer = new MockRestServer(port);
        mockServer.start();
    }

    @After
    public void tearDown() {
        // 2) Stop mock server after each test
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    /**
     * Test that serviceAlive(...) = true if we serve {"status":"UP"} at /actuator/health/livenessState
     */
    public void testServiceAliveUp() throws Exception {
        // Provide minimal JSON for "health"
        String jsonHealthUp = "{\"status\":\"UP\"}";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl(
                "/actuator/health/livenessState",
                jsonHealthUp
        );

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        boolean up = retriever.serviceAlive("http://localhost:" + port);
        assertTrue("Expected serviceAlive(...) to be true", up);

        mockServer.removeContext(ctx);
    }

    /**
     * Test that serviceAlive(...) = false or throws an exception if the JSON is not "UP",
     * or the endpoint is invalid. If your code throws an exception for a bad host,
     * adapt the assertion accordingly.
     */
    public void testServiceAliveDown() throws Exception {
        // Return "DOWN" or some mismatch
        String jsonHealthDown = "{\"status\":\"DOWN\"}";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl(
                "/actuator/health/livenessState",
                jsonHealthDown
        );

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        // If your code might throw or just parse and see "DOWN",
        // you can either check if it returns false or expect an exception
        boolean up = retriever.serviceAlive("http://localhost:" + port);
        assertFalse("Expected serviceAlive(...) to be false", up);

        mockServer.removeContext(ctx);
    }

    /**
     * Test that an unreachable URL throws IOException or returns false,
     * depending on how your code handles it.
     */
    public void testServiceAliveBadUrl() {
        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        try {
            boolean up = retriever.serviceAlive("http://no-such-server-hopefully");
            // If your code returns false for a bad URL, do:
            assertFalse("Expected false for unreachable URL", up);
        } catch (IOException | InterruptedException e) {
            // If your code rethrows an exception, you can pass here
            // or do "assertTrue(e instanceof ConnectException)" etc.
            System.out.println("Got exception for unreachable URL => OK: " + e);
        }
    }
    // Extra test: empty JSON for restaurants => means no restaurants
    public void testFetchRestaurants_EmptyJson() throws Exception {
        String emptyJson = "[]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/restaurants", emptyJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        Restaurant[] rests = retriever.fetchRestaurants("http://localhost:" + port);
        assertEquals(0, rests.length);

        mockServer.removeContext(ctx);
    }

    // Extra test: valid "centralArea" with no 'vertices'
    public void testFetchCentralAreaEmptyVertices() throws Exception {
        String centralJson = "{\"name\":\"central\",\"vertices\":[]}";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/centralArea", centralJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        NamedRegion region = retriever.fetchCentralArea("http://localhost:" + port);
        assertEquals("central", region.name());
        assertEquals(0, region.vertices().length);

        mockServer.removeContext(ctx);
    }



    // Extra test: No-Fly zones => empty => returns 0
    public void testFetchNoFlyZonesEmpty() throws Exception {
        String emptyJson = "[]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/noFlyZones", emptyJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        NamedRegion[] zones = retriever.fetchNoFlyZones("http://localhost:" + port);
        assertEquals(0, zones.length);

        mockServer.removeContext(ctx);
    }

    // Extra test: invalid JSON for restaurants => parse error
    public void testFetchRestaurantsInvalidJson() throws Exception {
        String invalidJson = "{]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/restaurants", invalidJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        try {
            retriever.fetchRestaurants("http://localhost:" + port);
            fail("Expected parse error or IOException");
        } catch (IOException e) {
            // pass
        }

        mockServer.removeContext(ctx);
    }

    // If your code fetches orders by GET /orders then filters by date in memory:
    public void testFetchOrdersEmptyJson() throws Exception {
        String emptyJson = "[]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/orders", emptyJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        Order[] orders = retriever.fetchOrders("http://localhost:" + port, "2025-02-01");
        // With empty array => obviously 0
        assertEquals(0, orders.length);

        mockServer.removeContext(ctx);
    }

    /**
     * Test fetching restaurants with a minimal valid JSON array.
     */
    public void testFetchRestaurants() throws Exception {
        // minimal JSON array of 1 restaurant
        String json = "[{" +
                "\"name\":\"TestR\"," +
                "\"location\":{\"lng\":-3.18,\"lat\":55.94}," +
                "\"openingDays\":[\"MONDAY\"]," +
                "\"menu\":[]" +
                "}]";
        HttpContext ctxRest = mockServer.getContextToServeDataOnUrl("/restaurants", json);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        Restaurant[] rests = retriever.fetchRestaurants("http://localhost:" + port);
        assertEquals("Should find exactly 1 restaurant", 1, rests.length);
        assertEquals("TestR", rests[0].name());

        mockServer.removeContext(ctxRest);
    }

    /**
     * Test retrieving orders with minimal JSON array.
     * We'll do a single order with "orderDate": "2025-02-01" so it can pass your filter.
     */
    public void testFetchOrders() throws Exception {
        String ordersJson = "[{" +
                "\"orderNo\":\"ABC123\"," +
                "\"orderDate\":\"2025-02-01\"," +
                "\"orderStatus\":\"UNDEFINED\"," +
                "\"orderValidationCode\":\"UNDEFINED\"," +
                "\"priceTotalInPence\":1500," +
                "\"pizzasInOrder\":[{" +
                "\"name\":\"TestPizza\",\"priceInPence\":1400" +
                "}]," +
                "\"creditCardInformation\":{" +
                "\"creditCardNumber\":\"1234567812345678\"," +
                "\"creditCardExpiry\":\"02/25\"," +
                "\"cvv\":\"123\"" +
                "}" +
                "}]";

        HttpContext ctxOrders = mockServer.getContextToServeDataOnUrl("/orders", ordersJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        Order[] allOrders = retriever.fetchOrders("http://localhost:" + port, "2025-02-01");
        assertEquals(1, allOrders.length);
        assertEquals("ABC123", allOrders[0].getOrderNo());

        mockServer.removeContext(ctxOrders);
    }

    /**
     * Test retrieving centralArea with minimal JSON
     */
    public void testFetchCentralArea() throws Exception {
        String centralJson = "{\"name\":\"central\",\"vertices\":[{\"lng\":-3.192473,\"lat\":55.946233}]}";
        HttpContext ctxCentral = mockServer.getContextToServeDataOnUrl("/centralArea", centralJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        NamedRegion central = retriever.fetchCentralArea("http://localhost:" + port);
        assertEquals("central", central.name());
        assertEquals("One vertex expected", 1, central.vertices().length);

        mockServer.removeContext(ctxCentral);
    }

    /**
     * Test retrieving noFlyZones with minimal array
     */
    public void testFetchNoFlyZones() throws Exception {
        String noFlyJson = "[{" +
                "\"name\":\"ZoneA\"," +
                "\"vertices\":[{\"lng\":-3.19,\"lat\":55.94},{\"lng\":-3.19,\"lat\":55.93}]" +
                "}]";
        HttpContext ctxNoFly = mockServer.getContextToServeDataOnUrl("/noFlyZones", noFlyJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        NamedRegion[] zones = retriever.fetchNoFlyZones("http://localhost:" + port);
        assertEquals(1, zones.length);
        assertEquals("ZoneA", zones[0].name());
        assertEquals(2, zones[0].vertices().length);

        mockServer.removeContext(ctxNoFly);
    }

    /**
     * Test that invalid JSON triggers an exception, if your code fails to parse
     * or we get 200 but with malformed JSON => Jackson parse error
     */
    public void testInvalidJsonFromServer() throws Exception {
        String badJson = "{]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/restaurants", badJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        try {
            retriever.fetchRestaurants("http://localhost:" + port);
            fail("Expected Jackson parse error or IOException for invalid JSON");
        } catch (IOException e) {
            // pass => we got the parse error
            System.out.println("Got expected error: " + e);
        } catch (Exception ex) {
            System.out.println("Got a different exception, but still an error: " + ex);
        }

        mockServer.removeContext(ctx);
    }

    public void testFetchCentralAreaInvalidJson() throws Exception {
        // serve invalid JSON at /centralArea
        String invalidJson = "{]";
        HttpContext ctx = mockServer.getContextToServeDataOnUrl("/centralArea", invalidJson);

        ApiDataRetriever retriever = ApiDataRetriever.getInstance();
        try {
            retriever.fetchCentralArea("http://localhost:" + port);
            fail("Expected parse error / IOException for invalid JSON");
        } catch (IOException e) {
            // pass
        }

        mockServer.removeContext(ctx);
    }
}
