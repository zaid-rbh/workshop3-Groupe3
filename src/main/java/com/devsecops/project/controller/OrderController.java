package com.devsecops.project.controller;

import com.devsecops.project.dto.CheckoutForm;
import com.devsecops.project.model.CartItem;
import com.devsecops.project.model.Order;
import com.devsecops.project.model.User;
import com.devsecops.project.service.CartService;
import com.devsecops.project.service.OrderService;
import com.devsecops.project.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    public OrderController(OrderService orderService, CartService cartService, UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Principal principal, Model model) {
        User user = userService.getByUsername(principal.getName());
        List<CartItem> items = cartService.getCart(user.getId());
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.total(items));
        model.addAttribute("user", user);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String checkout(Principal principal, @ModelAttribute CheckoutForm form,
                           RedirectAttributes redirectAttributes) {
        User user = userService.getByUsername(principal.getName());
        Order order = orderService.placeOrder(user, form);
        redirectAttributes.addFlashAttribute("message", "Order " + order.getOrderRef() + " placed (simulated).");
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/orders")
    public String orders(Principal principal, Model model) {
        User user = userService.getByUsername(principal.getName());
        model.addAttribute("orders", orderService.findForUser(user.getId()));
        return "orders";
    }

    /**
     * VULNERABILITY #10: insecure direct object reference (IDOR).
     * Any logged-in user can read any order just by changing the number in the URL.
     */
    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        return "order-detail";
    }
}
