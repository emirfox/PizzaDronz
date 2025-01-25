package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.*;

import java.util.*;

import static uk.ac.ed.inf.ilp.constant.OrderStatus.DELIVERED;

/**
 * Optimizes drone routes for delivering orders.
 */
public class RouteOptimizer {
    private final NamedRegion[] noFlyZones;
    private final NamedRegion centralArea;
    private final Restaurant[] restaurants;
    private final List<Order> orders;

    /**
     * Constructor to initialize the RouteOptimizer with necessary data.
     * @param noFlyZones Array of no-fly zones to avoid.
     * @param centralArea Central area for the drone operations.
     * @param restaurants Array of available restaurants.
     * @param orders List of orders to be delivered.
     */
    public RouteOptimizer(NamedRegion[] noFlyZones, NamedRegion centralArea, Restaurant[] restaurants, List<Order> orders) {
        this.noFlyZones = noFlyZones;
        this.centralArea = centralArea;
        this.restaurants = restaurants;
        this.orders = orders;
    }

    /**
     * Finds the location of the restaurant for a given order.
     * @param order The order for which to find the restaurant location.
     * @return The location (LngLat) of the restaurant.
     */
    private LngLat findRestaurantLocation(Order order) {
        // Collect the order's pizza names (trimmed)
        List<Pizza> orderPizzas = Arrays.asList(order.getPizzasInOrder());
        Set<String> orderPizzaNames = new HashSet<>();
        for (Pizza p : orderPizzas) {
            // .name().trim() to remove trailing spaces or newlines
            orderPizzaNames.add(p.name().trim());
        }

        // Check each restaurant
        for (Restaurant restaurant : restaurants) {
            List<Pizza> restaurantMenu = Arrays.asList(restaurant.menu());
            Set<String> restaurantPizzaNames = new HashSet<>();
            for (Pizza p : restaurantMenu) {
                restaurantPizzaNames.add(p.name().trim());
            }

            // Now see if this restaurant covers ALL the pizzas in the order
            if (restaurantPizzaNames.containsAll(orderPizzaNames)) {
                return restaurant.location();
            }
        }

        throw new IllegalArgumentException("Restaurant location for the order not found. OrderNo="
                + order.getOrderNo());
    }


    /**
     * Optimizes and calculates routes for all orders.
     * @return A list of DroneMovement objects representing the optimized routes.
     */
    public List<DroneMovement> optimizeRoutes() {
        DronePathPlanner planner = new DronePathPlanner(noFlyZones, centralArea);
        List<DroneMovement> allRoutes = new ArrayList<>();

        LngLat deliveryPoint = new LngLat(-3.186874, 55.944494); // Appleton Tower coordinates
        for (Order order : orders) {
            LngLat restaurantLocation = findRestaurantLocation(order);

            // Calculate the round trip path for each order
            List<DroneMovement> roundTripRoute = planner.findTotalPath(deliveryPoint, restaurantLocation, order.getOrderNo());
            allRoutes.addAll(roundTripRoute);

            order.setOrderStatus(DELIVERED); // Mark the order as delivered
        }

        return allRoutes;
    }
}
