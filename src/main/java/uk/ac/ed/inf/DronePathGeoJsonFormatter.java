package uk.ac.ed.inf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * Class to format drone paths into a GeoJSON structure.
 */
public class DronePathGeoJsonFormatter {

    /**
     * Converts a list of drone movements into a GeoJSON string representing their flight path.
     * @param movements List of drone movements to be formatted.
     * @return A GeoJSON string representing the flight path of the drone.
     */
    public static String formatPathToGeoJson(List<DroneMovement> movements) {
        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");

        JsonArray features = new JsonArray();
        JsonObject feature = new JsonObject();
        feature.addProperty("type", "Feature");

        // Empty properties object
        JsonObject properties = new JsonObject();
        feature.add("properties", properties);

        JsonObject geometry = new JsonObject();
        geometry.addProperty("type", "LineString");
        JsonArray coordinates = new JsonArray();

        for (DroneMovement movement : movements) {
            JsonArray point = new JsonArray();
            point.add(movement.getStart().lng());
            point.add(movement.getStart().lat());
            coordinates.add(point);
        }

        geometry.add("coordinates", coordinates);
        feature.add("geometry", geometry);
        features.add(feature);
        featureCollection.add("features", features);

        return featureCollection.toString();
    }
}
