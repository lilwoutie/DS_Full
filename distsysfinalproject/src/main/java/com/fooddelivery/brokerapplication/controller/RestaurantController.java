/*
package com.fooddelivery.brokerapplication.controller;

import com.fooddelivery.brokerapplication.model.Meal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for rendering the restaurant menu page.
 */
/*
@Controller
public class RestaurantController {

    /**
     * GET /restaurant/{id}
     * Populates the model with:
     *  - restaurantName
     *  - restaurantDescription
     *  - List<Meal> meals
     */
/*
    @GetMapping("/restaurant/{id}")
    public String restaurantMenu(@PathVariable("id") int id, Model model) {
        String restaurantName;
        String restaurantDescription;

        // Example: two restaurants with simple metadata.
        if (id == 1) {
            restaurantName = "Rest A";
            restaurantDescription = "A selection of delicious meals.";
        } else {
            restaurantName = "Rest B";
            restaurantDescription = "Tasty specialties to satisfy your cravings.";
        }

        // Dummy hard‐coded meal list for illustration.
        List<Meal> meals = Arrays.asList(
                new Meal(101L, "Margherita Pizza",
                        "Classic pizza with tomatoes and basil",
                        12.00),
                new Meal(102L, "Pasta Bolognese",
                        "Pasta with a rich meat sauce",
                        15.00),
                new Meal(103L, "Caesar Salad",
                        "Crisp romaine with parmesan and croutons",
                        10.00)
        );

        model.addAttribute("restaurantId", id);
        model.addAttribute("restaurantName", restaurantName);
        model.addAttribute("restaurantDescription", restaurantDescription);
        model.addAttribute("meals", meals);

        return "restaurant";  // Thymeleaf resolves to restaurant.html
    }
}
*/
package com.fooddelivery.brokerapplication.controller;

import com.fooddelivery.brokerapplication.model.Meal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Controller
public class RestaurantController {

    private final RestTemplate restTemplate;

    @Autowired
    public RestaurantController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/restaurant/{id}")
    public String restaurantMenu(@PathVariable("id") int id, Model model) {
        String restaurantName;
        String restaurantDescription;

        // Example: two suppliers mapped by ID
        String supplierBaseUrl;
        if (id == 1) {
            restaurantName = "Rest A";
            restaurantDescription = "A selection of delicious meals.";
            supplierBaseUrl = "http://localhost:8081"; // Supplier 1
        } else {
            restaurantName = "Rest B";
            restaurantDescription = "Tasty specialties to satisfy your cravings.";
            supplierBaseUrl = "http://localhost:8082"; // Supplier 2 (if exists)
        }

        // Fetch available products
        String productUrl = supplierBaseUrl + "/products"; // Only available ones
        ResponseEntity<Product[]> response = restTemplate.getForEntity(productUrl, Product[].class);
        Product[] products = response.getBody();

        List<Meal> meals = Arrays.stream(products)
                .map(p -> new Meal(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice()
                ))
                .collect(Collectors.toList());

        model.addAttribute("restaurantId", id);
        model.addAttribute("restaurantName", restaurantName);
        model.addAttribute("restaurantDescription", restaurantDescription);
        model.addAttribute("meals", meals);

        return "restaurant";
    }

    // Inner static class to map product JSON
    static class Product {
        private Long id;
        private String name;
        private String description;
        private double price;

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }

        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setPrice(double price) { this.price = price; }
    }
}
