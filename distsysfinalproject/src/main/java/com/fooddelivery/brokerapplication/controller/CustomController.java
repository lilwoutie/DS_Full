package com.fooddelivery.brokerapplication.controller;
import com.fooddelivery.brokerapplication.model.Cart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("cart")
public class CustomController {

    // Initialize Cart in session (even if empty)
    @ModelAttribute("cart")
    public Cart cart() {
        return new Cart();
    }

    // Map GET “/” to home.html
    @GetMapping("/")
    public String home(@ModelAttribute("cart") Cart cart, Model model) {
        // We’re not showing a cart count for now
        return "home";   // corresponds to src/main/resources/templates/home.html
    }
}

