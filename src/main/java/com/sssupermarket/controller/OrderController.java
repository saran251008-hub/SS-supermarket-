package com.sssupermarket.controller;

import com.sssupermarket.dto.CartResponse;
import com.sssupermarket.entity.Order;
import com.sssupermarket.exception.EmptyCartException;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.CartService;
import com.sssupermarket.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(CartService cartService, OrderService orderService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @GetMapping("/checkout")
    public String checkout(Model model, RedirectAttributes redirectAttributes) {
        CartResponse cart = cartService.getCart(currentUser.getId());
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty. Add products before checking out.");
            return "redirect:/cart";
        }
        model.addAttribute("cart", cart);
        return "checkout";
    }

    @PostMapping("/checkout/confirm")
    public String confirmOrder(RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.createOrder(currentUser.getId());
            redirectAttributes.addFlashAttribute("orderId", order.getId());
            redirectAttributes.addFlashAttribute("totalAmount", order.getTotalAmount());
            redirectAttributes.addFlashAttribute("orderStatus", order.getStatus().name());
            return "redirect:/order-success";
        } catch (EmptyCartException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        } catch (InsufficientStockException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/order-success")
    public String orderSuccess(Model model) {
        if (!model.containsAttribute("orderId")) {
            return "redirect:/products";
        }
        return "order-success";
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getOrdersForUser(currentUser.getId()));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetails(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderForUser(currentUser.getId(), id));
        return "order-details";
    }
}
