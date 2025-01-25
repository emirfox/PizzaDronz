package uk.ac.ed.inf;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.ed.inf.ilp.data.Order;

/**
 * Class to format delivery information into a JSON structure.
 */
public class OrderDeliveryJsonFormatter {

    /**
     * Converts an array of orders into a JSON string representing their delivery details.
     * @param orders Array of orders to be formatted.
     * @return A JSON string representing the delivery details of the orders.
     */
    public static String formatDeliveriesToJson(Order[] orders) {
        JSONArray deliveriesJsonArray = new JSONArray();
        for (Order order : orders) {
            JSONObject deliveryJson = new JSONObject();
            deliveryJson.put("orderNo", order.getOrderNo());
            deliveryJson.put("orderStatus", order.getOrderStatus().toString());
            deliveryJson.put("orderValidationCode", order.getOrderValidationCode().toString());
            deliveryJson.put("costInPence", order.getPriceTotalInPence());
            deliveriesJsonArray.put(deliveryJson);
        }
        return deliveriesJsonArray.toString();
    }
}
