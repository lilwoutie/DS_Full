package com.fooddelivery.brokerapplication.controller;

import com.fooddelivery.brokerapplication.model.Cart;
import com.fooddelivery.brokerapplication.model.CartItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@SessionAttributes("cart")
public class CartController {

    @ModelAttribute("cart")
    public Cart cart() {
        return new Cart();
    }

    @GetMapping("/cart")
    public String viewCart(@ModelAttribute("cart") Cart cart, Model model) {
        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", cart.getTotalAmount());
        return "cart";
    }

    @PostMapping("/cart/add")
    @ResponseBody
    public String addItemsToCart(@RequestBody List<CartItem> items,
                                 @ModelAttribute("cart") Cart cart) {
        for (CartItem incomingItem : items) {
            // Log for debugging:
            System.out.println("[DEBUG] Adding to cart: mealId=" + incomingItem.getMealId()
                    + ", restaurantId=" + incomingItem.getRestaurantId()
                    + ", name=" + incomingItem.getName()
                    + ", quantity=" + incomingItem.getQuantity());
            cart.addItem(incomingItem);
        }
        return "success";
    }


    @PostMapping("/cart/update")
    public String updateCartItem(@RequestParam Long mealId,
                                 @RequestParam int quantity,
                                 @ModelAttribute("cart") Cart cart) {
        if (quantity <= 0) {
            cart.removeItem(mealId);
        } else {
            for (CartItem item : cart.getItems()) {
                if (item.getMealId().equals(mealId)) {
                    item.setQuantity(quantity);
                    break;
                }
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(@ModelAttribute("cart") Cart cart) {
        cart.clear();
        return "redirect:/cart";
    }

    @GetMapping("/cart/count")
    @ResponseBody
    public int getCartItemCount(@ModelAttribute("cart") Cart cart) {
        return cart.getTotalItemCount();
    }

    @PostMapping("/cart/checkout")
    public String proceedToCheckout() {
        return "redirect:/checkout";
    }
}
