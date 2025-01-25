package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main class of the drone delivery service application.
 *
 * Has:
 *  - public static void main(...) for normal usage
 *  - public static void runEverything(...) for JUnit test usage
 */
public class App {

    /**
     * Normal entry point when running "java -jar".
     * Wraps runEverything(...) so on exception we do System.exit(1).
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            runEverything(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The method your tests call (instead of main) so they can catch exceptions
     * and verify outputs without killing the JVM with System.exit.
     */
    public static void runEverything(String[] args) throws IOException, InterruptedException {
        // 1) Check argument count
        if (args.length != 2) {
            throw new IllegalArgumentException("Argument error: Two arguments required - date and API URL");
        }
        String date = args[0];
        String url = args[1];

        // 2) Validate date
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date error: Date must be in YYYY-MM-DD format");
        }
        // 3) Validate url
        if (!url.matches("https://.*")) {
            throw new IllegalArgumentException("URL error: URL must begin with 'https://'");
        }

        // 4) Check server health
        boolean serviceUp = ApiDataRetriever.getInstance().serviceAlive(url);
        if (!serviceUp) {
            throw new IllegalStateException("Service error: Service is not responding");
        }

        // 5) Fetch data
        Restaurant[] restaurants = ApiDataRetriever.getInstance().fetchRestaurants(url);
        Order[] orders = ApiDataRetriever.getInstance().fetchOrders(url, date);
        NamedRegion centralArea = ApiDataRetriever.getInstance().fetchCentralArea(url);
        NamedRegion[] noFlyZones = ApiDataRetriever.getInstance().fetchNoFlyZones(url);

        // 6) Validate & process orders
        List<Order> validOrders = validateOrders(orders, restaurants);

        RouteOptimizer optimizer = new RouteOptimizer(noFlyZones, centralArea, restaurants, validOrders);
        List<DroneMovement> paths = optimizer.optimizeRoutes();

        // 7) Output
        String year = date.substring(0,4);
        String month = date.substring(5,7);
        String day = date.substring(8,10);

        new File("resultfiles").mkdirs();

        writeDeliveryJson(orders, year, month, day);
        writeFlightpathJson(paths, year, month, day);
        writeGeoJson(paths, year, month, day);
    }

    private static List<Order> validateOrders(Order[] orders, Restaurant[] restaurants) {
        OrderValidationImpl validator = new OrderValidationImpl();
        List<Order> valid = new ArrayList<>();
        for (Order o : orders) {
            Order checked = validator.validateOrder(o, restaurants);
            if (checked.getOrderStatus() != OrderStatus.INVALID) {
                valid.add(o);
            }
        }
        return valid;
    }

    private static void writeDeliveryJson(Order[] orders, String y, String m, String d) throws IOException {
        try (FileWriter fw = new FileWriter("resultfiles/deliveries-" + y + "-" + m + "-" + d + ".json")) {
            fw.write(OrderDeliveryJsonFormatter.formatDeliveriesToJson(orders));
        }
    }

    private static void writeFlightpathJson(List<DroneMovement> paths, String y, String m, String d) throws IOException {
        try (FileWriter fw = new FileWriter("resultfiles/flightpath-" + y + "-" + m + "-" + d + ".json")) {
            fw.write(DroneFlightpathJsonFormatter.formatFlightpathsToJson(paths));
        }
    }

    private static void writeGeoJson(List<DroneMovement> paths, String y, String m, String d) throws IOException {
        try (FileWriter fw = new FileWriter("resultfiles/drone-" + y + "-" + m + "-" + d + ".geojson")) {
            fw.write(DronePathGeoJsonFormatter.formatPathToGeoJson(paths));
        }
    }
}
