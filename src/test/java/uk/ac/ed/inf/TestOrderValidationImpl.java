package uk.ac.ed.inf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;
import uk.ac.ed.inf.ilp.constant.SystemConstants;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrderValidationImpl focusing on valid/invalid orders,
 * payment details, pizza availability, restaurant scheduling, etc.
 */

public class TestOrderValidationImpl {

    private OrderValidationImpl validator;
    private Restaurant[] mockRestaurants;

    // Example pizzas for testing
    private final Pizza pizza_1 = new Pizza("Pizza_1", 200);
    private final Pizza pizza_2 = new Pizza("Pizza_2", 150);

    // We'll define a "valid" expiry date string like "12/30"
    // so the code won't mark it as expired.
    private final String validExpiryDate = "12/30";

    @BeforeEach
    void setUp() {
        validator = new OrderValidationImpl();

        // Single or multiple restaurants
        // We'll combine the approach so we can test single & multi-restaurant logic.
        Restaurant r1 = new Restaurant(
                "Rest_1",
                new LngLat(-3.19, 55.94),
                new DayOfWeek[]{
                        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                },
                new Pizza[]{pizza_1} // Only "Pizza_1"
        );
        Restaurant r2 = new Restaurant(
                "Rest_2",
                new LngLat(-3.19, 55.94),
                new DayOfWeek[]{
                        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                },
                new Pizza[]{pizza_2} // Only "Pizza_2"
        );

        mockRestaurants = new Restaurant[]{r1, r2};
    }

    // Helper to create a credit card with default valid 16-digit number & 3-digit CVV
    private CreditCardInformation makeCreditCard(String number, String expiry, String cvv) {
        return new CreditCardInformation(number, expiry, cvv);
    }


    @Test
    void testValidOrder_SingleRestaurant() {
        // We'll assume pizza_1 is from "Rest_1"
        // Price for pizza_1 is 200, plus order charge 100 => 300 total
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "123");

        // The correct total is 200 + 100 = 300
        Order order = new Order(
                "ORDER01",
                LocalDate.of(2025, 1, 23), // arbitrary future date
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                300,
                pizzas,
                card
        );

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.VALID_BUT_NOT_DELIVERED, result.getOrderStatus());
        assertEquals(OrderValidationCode.NO_ERROR, result.getOrderValidationCode());
    }

    @Test
    void testPizzaNotDefined() {
        // Pizzas that aren't in the restaurants' menus
        // "R2: Meat Lover" does not exist in either mock restaurant
        Pizza[] pizzas = {
                new Pizza("R2: Meat Lover", 1400)
        };
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "123");
        // Suppose total is 1500 but code sees mismatch anyway
        Order order = new Order(
                "ORDER02",
                LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                1500,
                pizzas,
                card
        );

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.PIZZA_NOT_DEFINED, result.getOrderValidationCode());
    }

    @Test
    void testMaxPizzaCountExceeded() {
        // If SystemConstants.MAX_PIZZAS_PER_ORDER = 4, let's try 5
        Pizza[] fivePizzas = {
                pizza_1, pizza_1, pizza_1, pizza_1, pizza_1
        };
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "123");
        // each pizza_1 is 200, 5 => 1000 plus order charge 100 => 1100
        // but we expect invalid due to too many pizzas
        Order order = new Order("ORDER03", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                1100, fivePizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, result.getOrderValidationCode());
    }

    @Test
    void testCardNumberInvalid() {
        // less than 16 digits
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234", "05/25", "123");
        // total 200 + 100 => 300
        Order order = new Order("ORDER04", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                300, pizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.CARD_NUMBER_INVALID, result.getOrderValidationCode());
    }

    @Test
    void testCvvInvalid() {
        // CVV not 3 digits
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "12");
        // total = 300
        Order order = new Order("ORDER05", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                300, pizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.CVV_INVALID, result.getOrderValidationCode());
    }

    @Test
    void testWrongPizza() {
        // "New Pizza" doesn't belong to either restaurant
        Pizza[] pizzas = {new Pizza("New Pizza", 300)};
        Order order = new Order(
                "OrderID",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                400, // expecting 300 + 100 if it were valid
                pizzas,
                makeCreditCard("1234567812345678", "05/25", "123")
        );
        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.PIZZA_NOT_DEFINED, result.getOrderValidationCode());
    }

    @Test
    void testMultipleRestaurantPizzas() {
        // One pizza from Rest_1 and one from Rest_2 => PIZZA_FROM_MULTIPLE_RESTAURANTS
        // pizza_1 belongs to r1, pizza_2 to r2
        Pizza[] pizzas = {pizza_1, pizza_2};
        // total = 200 + 150 + 100 => 450 if it were valid
        Order order = new Order(
                "OrderID",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                450,
                pizzas,
                makeCreditCard("1234567812345678", "05/25", "123")
        );
        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, result.getOrderValidationCode());
    }

    @Test
    void testWrongTotalPrice() {
        // If the order has 1 pizza_1 => 200 plus order charge 100 => 300
        // We incorrectly set 200 => should be invalid for TOTAL_INCORRECT
        Pizza[] pizzas = {pizza_1};
        Order order = new Order(
                "OrderID",
                LocalDate.now(),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                200, // incorrect
                pizzas,
                makeCreditCard("1234567812345678", "05/25", "123")
        );
        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.TOTAL_INCORRECT, result.getOrderValidationCode());
    }

    @Test
    void testExpiredCreditCard() {
        // Make a date in the past e.g. "01/22" if today is 2023/24
        // For safety let's do "01/23" if we want it definitely expired
        // The code adds 1 month, so it becomes "2023-02-01"
        // If our order date is 2023-03-01 => expired
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234567812345678", "01/23", "123");

        // total = 200 + 100 => 300
        // Let's say the order date is 2023-03-15, definitely after 2023-02-01
        Order order = new Order(
                "OrderExpiredCard",
                LocalDate.of(2023, 3, 15),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                300,
                pizzas,
                card
        );
        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.EXPIRY_DATE_INVALID, result.getOrderValidationCode());
    }




    @Test
    void testInvalidCvv() {
        // CVV not 3 digits
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "1234");
        // total = 300
        Order order = new Order("ORDER05", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                300, pizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.CVV_INVALID, result.getOrderValidationCode());
    }

    @Test
    void invalidCard_number() {
        // less than 16 digits
        Pizza[] pizzas = {pizza_1};
        CreditCardInformation card = makeCreditCard("1234", "05/25", "123");
        // total 200 + 100 => 300
        Order order = new Order("ORDER04", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                300, pizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.CARD_NUMBER_INVALID, result.getOrderValidationCode());
    }


    @Test
    void invalid_pizza_count(){
// If SystemConstants.MAX_PIZZAS_PER_ORDER = 4, let's try 5
        Pizza[] fivePizzas = {
                pizza_1, pizza_1, pizza_1, pizza_1, pizza_1
        };
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "123");
        // each pizza_1 is 200, 5 => 1000 plus order charge 100 => 1100
        // but we expect invalid due to too many pizzas
        Order order = new Order("ORDER03", LocalDate.of(2025, 1, 23),
                OrderStatus.UNDEFINED, OrderValidationCode.UNDEFINED,
                1100, fivePizzas, card);

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, result.getOrderValidationCode());

    }
    public void testRestaurantClosed() {
        // We'll pick a pizza that is on "Civerinos Slice" menu => "R1: Margarita"
        Pizza[] pizzas = { pizza_1 };
        CreditCardInformation card = makeCreditCard("1234567812345678","05/25","123");

        // Suppose the date is a WEDNESDAY (2025-01-29 is indeed a Wednesday)
        Order order = new Order(
                "ORDER06",
                LocalDate.of(2025, 1, 29),
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                1100,  // e.g. 1000 for pizza + 100 order charge
                pizzas,
                card
        );

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.RESTAURANT_CLOSED, result.getOrderValidationCode());
    }


    @Test
    void testWrongItemsInRestaurant() {
        // Suppose user tries "R1: Margarita" + "R2: Meat Lover" => from different restaurants
        Pizza[] pizzas = { pizza_1, pizza_2 };
        // total => 1000 + 1400 + 100 = 2500
        CreditCardInformation card = makeCreditCard("1234567812345678", "05/25", "123");

        // A date that one restaurant is open, say Sunday => "Civerinos Slice" is open
        // But "Wed-Thu Only" is not open => also this triggers 'PIZZA_FROM_MULTIPLE_RESTAURANTS'
        Order order = new Order(
                "ORDER08",
                LocalDate.of(2025, 1, 26), // Sunday
                OrderStatus.UNDEFINED,
                OrderValidationCode.UNDEFINED,
                2500,
                pizzas,
                card
        );

        Order result = validator.validateOrder(order, mockRestaurants);
        assertEquals(OrderStatus.INVALID, result.getOrderStatus());
        assertEquals(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, result.getOrderValidationCode());
    }



}






