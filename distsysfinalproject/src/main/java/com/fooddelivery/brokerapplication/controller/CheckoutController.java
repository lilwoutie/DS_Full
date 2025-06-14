package com.fooddelivery.brokerapplication.controller;

import com.fooddelivery.brokerapplication.model.Cart;
import com.fooddelivery.brokerapplication.model.CartItem;
import com.fooddelivery.brokerapplication.model.Order;
import com.fooddelivery.brokerapplication.model.OrderForm;
import com.fooddelivery.brokerapplication.service.OrderService;
import com.fooddelivery.brokerapplication.service.TwoPhaseCommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@SessionAttributes("cart")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private TwoPhaseCommitService twoPhaseCommitService;

    @ModelAttribute("cart")
    public Cart cart() {
        return new Cart();
    }

    @GetMapping("/checkout")
    public String checkoutForm(Model model, @ModelAttribute("cart") Cart cart) {
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("orderForm", new OrderForm());
        model.addAttribute("total", cart.getTotalAmount());
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@ModelAttribute OrderForm orderForm,
                             @ModelAttribute("cart") Cart cart,
                             Model model) {
        List<CartItem> items = cart.getItems();
        double total = cart.getTotalAmount();
        logger.info("Starting checkout for {} items, total={}", items.size(), total);

        try {
            // 1) Execute distributed transaction across restaurant services
            twoPhaseCommitService.executeTransaction(items);
            // If no exception: all participants prepared and committed

            // 2) Persist the final Order in broker database
            Order orderEntity = new Order();
            orderEntity.setCustomerName(orderForm.getFullName());
            orderEntity.setAddress(orderForm.getAddress());
            orderEntity.setDeliveryOption(orderForm.getDeliveryMethod());
            orderEntity.setPaymentMethod(orderForm.getPaymentMethod());
            orderEntity.setMeals(items);
            orderService.saveOrder(orderEntity);
            logger.info("Order persisted locally after successful 2PC");

            // 3) Clear the cart
            cart.clear();

            // 4) Redirect to confirmation page
            return "redirect:/confirmation?total=" + total;

        } catch (Exception ex) {
            // Distributed transaction aborted
            logger.warn("Distributed transaction aborted during checkout: {}", ex.getMessage());
            // Prepare model to re-display checkout with error message
            model.addAttribute("errorMessage",
                    "Sorry, we could not process your order at this time. Please try again later.");
            model.addAttribute("total", total);
            model.addAttribute("orderForm", orderForm);
            // Cart remains intact so user can retry or modify
            return "checkout";
        }
    }

    @GetMapping("/confirmation")
    public String confirmOrder(@RequestParam double total, Model model) {
        model.addAttribute("total", total);
        return "confirmation";
    }
}
