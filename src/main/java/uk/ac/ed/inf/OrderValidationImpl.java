package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.constant.SystemConstants;

import java.time.LocalDate;
import java.util.HashMap;
import java.time.DayOfWeek;

/**
 * Implementation class for validating orders.
 * It checks order details against restaurant availability, credit card validity, and other criteria.
 */
public class OrderValidationImpl implements OrderValidation {

    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {

        HashMap<String, String> restaurantPerPizza = new HashMap<>();  // creating a hashmap to store the restaurant name for each pizza

        for (Restaurant restaurant : definedRestaurants) {
            for (Pizza pizza : restaurant.menu()) {
                restaurantPerPizza.put(pizza.name(), restaurant.name());   // adding the restaurant name for each pizza
            }
        }

        String orderRestaurantName = null;
        int pizzaCount = 0;   // counter for the number of pizzas in the order

        for (Pizza pizza : orderToValidate.getPizzasInOrder()) {
            String restaurantName = restaurantPerPizza.get(pizza.name());      // getting the ordered restaurant name for further checks

            if (restaurantName == null) {
                // Pizza not available in any restaurant
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                return orderToValidate;
            } else if (orderRestaurantName == null) {
                // First pizza in order, set the restaurant name
                orderRestaurantName = restaurantName;
            } else if (!orderRestaurantName.equals(restaurantName)) {
                // Pizza from a different restaurant
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
                return orderToValidate;
            }

            pizzaCount++;
            if (pizzaCount > SystemConstants.MAX_PIZZAS_PER_ORDER) {  // checking if the number of pizzas in the order exceeds the maximum
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
                return orderToValidate;
            }
        }
        //check the order date
        if (orderToValidate.getOrderDate().getDayOfWeek() == null) { // checking if the order date is valid
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            return orderToValidate;
        }
        //check the restaurant time match use orderResraurantName
        for (Restaurant restaurant : definedRestaurants) {
            if (restaurant.name().equals(orderRestaurantName)) {
                boolean isOpen = false;
                for (DayOfWeek dayOfWeek : restaurant.openingDays()) {
                    if (dayOfWeek.equals(orderToValidate.getOrderDate().getDayOfWeek())) {
                        isOpen = true;
                        break;
                    }
                }
                if (!isOpen) {                                    // checking if the restaurant is open on the order date
                    orderToValidate.setOrderStatus(OrderStatus.INVALID);
                    orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
                    return orderToValidate;
                }
            }
        }
        //check the credit card information

        CreditCardInformation ccInfo = orderToValidate.getCreditCardInformation();

        if (ccInfo.getCreditCardNumber().length() != 16) {  // checking if the credit card number is valid
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }
        //check if the input is a number and is 3 digits
        if (ccInfo.getCvv().length() != 3) {  // checking if the cvv is valid
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }


        // Check if the credit card is expired
        if (isCardExpired(ccInfo.getCreditCardExpiry(), orderToValidate.getOrderDate())) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        //check the price
        int totalPrice = 0;
        for (Pizza pizza : orderToValidate.getPizzasInOrder()) {
            totalPrice += pizza.priceInPence();
        }
        totalPrice += SystemConstants.ORDER_CHARGE_IN_PENCE;  // adding the order charge to the total prices

        if (totalPrice != orderToValidate.getPriceTotalInPence()) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            return orderToValidate;
        }
        //if everything is ok
        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);
        return orderToValidate;
    }

    // Helper method to check if the credit card is expired
    private static boolean isCardExpired(String cardExpiry, LocalDate orderDate) {
        int month = Integer.parseInt(cardExpiry.substring(0, 2));
        int year = Integer.parseInt(cardExpiry.substring(3, 5));
        if (month > 11) {
            month = 1;
            year += 1;
        } else {
            month += 1;
        }
        String reformattedExpiry = String.format("20%02d-%02d-01", year, month);
        LocalDate expiryDate = LocalDate.parse(reformattedExpiry);

        return !orderDate.isBefore(expiryDate);
    }

}