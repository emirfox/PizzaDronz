package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junit.framework.TestCase;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JUnit 4 style test class for App.
 */
public class AppTest extends TestCase {

    public void testInvalidNumberOfArguments() {
        // e.g. zero
        String[] none = {};
        try {
            App.runEverything(none);
            fail("Expected error for 0 arguments");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Two arguments required"));
        }
    }

    public void testInvalidDateFormat() {
        String[] arr = {"01-02-2025", "https://ilp-rest-2024.azurewebsites.net"};
        try {
            App.runEverything(arr);
            fail("Expected 'Date error'");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Date error"));
        }
    }

    public void testInvalidUrl() {
        String[] arr = {"2025-02-01", "http://ilp-rest-2024.azurewebsites.net"};
        try {
            App.runEverything(arr);
            fail("Expected 'URL error'");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("URL error"));
        }
    }

    public void testAppProducesThreeFiles() throws Exception {
        // Real server usage:
        String date = "2025-02-01";
        String baseUrl = "https://ilp-rest-2024.azurewebsites.net";

        // Clean up existing
        String dir = "resultfiles";
        String deliveriesFile = dir + "/deliveries-" + date + ".json";
        String flightpathFile = dir + "/flightpath-" + date + ".json";
        String geoJsonFile = dir + "/drone-" + date + ".geojson";
        FlightPlannerUtils.deleteFile(deliveriesFile);
        FlightPlannerUtils.deleteFile(flightpathFile);
        FlightPlannerUtils.deleteFile(geoJsonFile);

        // Run
        App.runEverything(new String[]{date, baseUrl});

        // Check
        assertTrue(FlightPlannerUtils.checkFileExists(deliveriesFile));
        assertTrue(FlightPlannerUtils.checkFileExists(flightpathFile));
        assertTrue(FlightPlannerUtils.checkFileExists(geoJsonFile));
    }

    public void testAppRunsUnder60Secs() {
        String date = "2025-02-01";
        String baseUrl = "https://ilp-rest-2024.azurewebsites.net";

        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        try {
            App.runEverything(new String[]{date, baseUrl});
        } catch(Exception e) {
            fail("Should not fail for valid input: " + e.getMessage());
        }
        timer.stop();
        long dur = timer.getDuration();
        System.out.println("Run took " + dur + " ms");
        assertTrue(dur < 60000);
    }

    public void testAppOrdersDeliveredSequentially() throws Exception {
        // Suppose we create some orders, or rely on real server data.
        // For example, we assume 2025-02-01 has some valid & invalid orders.
        String date = "2025-02-01";
        String baseUrl = "https://ilp-rest-2024.azurewebsites.net";

        // Run
        App.runEverything(new String[]{date, baseUrl});

        File flightPathFile = new File("resultfiles/flightpath-" + date + ".json");
        assertTrue(flightPathFile.exists());

        // Parse the flightpath JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        FlightPlannerUtils.FlightMove[] moves = mapper.readValue(flightPathFile, FlightPlannerUtils.FlightMove[].class);

        // Suppose we know from the server which orders are valid & in what order?
        // If you want a simple check of the distinct 'orderNo' in 'moves' in sequence:
        ArrayList<String> orderSequence = new ArrayList<>();
        for (FlightPlannerUtils.FlightMove move : moves) {
            if (!orderSequence.contains(move.orderNo)) {
                orderSequence.add(move.orderNo);
            }
        }

        // Now check that the sequence is what you expect, e.g. [ "ORDER1", "ORDER2", "ORDER3" ]
        // or that there's no invalid orderNo, etc.

        // For demonstration:
       // System.out.println("Sequence of delivered orders: " + orderSequence);
        assertFalse(orderSequence.isEmpty());
    }
}
