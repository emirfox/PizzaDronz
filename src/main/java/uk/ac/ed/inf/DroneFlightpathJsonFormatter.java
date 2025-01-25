package uk.ac.ed.inf;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * Class to format drone flight paths into a JSON structure.
 */
public class DroneFlightpathJsonFormatter {

    /**
     * Converts a list of drone movements into a JSON string representing the flight paths.
     * @param movements List of drone movements to be formatted.
     * @return A JSON string representing the flight paths of the drone.
     */
    public static String formatFlightpathsToJson(List<DroneMovement> movements) {
        JSONArray flightPathJsonArray = new JSONArray();
        for (DroneMovement movement : movements) {
            JSONObject flightPathJson = new JSONObject();
            flightPathJson.put("orderNo", movement.getOrderNo());
            flightPathJson.put("fromLongitude", movement.getStart().lng());
            flightPathJson.put("fromLatitude", movement.getStart().lat());
            flightPathJson.put("angle", movement.getAngle());
            flightPathJson.put("toLongitude", movement.getEnd().lng());
            flightPathJson.put("toLatitude", movement.getEnd().lat());
            flightPathJsonArray.put(flightPathJson);
        }
        return flightPathJsonArray.toString();
    }
}
