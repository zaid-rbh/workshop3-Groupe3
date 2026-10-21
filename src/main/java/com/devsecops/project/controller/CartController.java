package com.devsecops.project.controller;

import com.devsecops.project.model.CartItem;
import com.devsecops.project.model.User;
import com.devsecops.project.service.CartService;
import com.devsecops.project.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping
    public String view(Principal principal, Model model) {
        User user = userService.getByUsername(principal.getName());
        List<CartItem> items = cartService.getCart(user.getId());
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.total(items));
        return "cart";
    }

    /** VULNERABILITY #15: "price" and "quantity" are accepted from the browser without any check. */
    @PostMapping("/add")
    public String add(Principal principal,
                      @RequestParam Long productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) BigDecimal price,
                      RedirectAttributes redirectAttributes) {
        User user = userService.getByUsername(principal.getName());
        cartService.addToCart(user, productId, quantity, price);
        redirectAttributes.addFlashAttribute("message", "Product added to your cart.");
        return "redirect:/cart";
    }

    /** VULNERABILITY #10 (part): the cart item id is not checked against the logged-in user. */
    @PostMapping("/update")
    public String update(@RequestParam Long itemId, @RequestParam int quantity,
                         RedirectAttributes redirectAttributes) {
        cartService.updateQuantity(itemId, quantity);
        redirectAttributes.addFlashAttribute("message", "Cart updated.");
        return "redirect:/cart";
    }

    /** VULNERABILITY #10 (part): same missing ownership check. */
    @PostMapping("/remove")
    public String remove(@RequestParam Long itemId, RedirectAttributes redirectAttributes) {
        cartService.remove(itemId);
        redirectAttributes.addFlashAttribute("message", "Item removed from your cart.");
        return "redirect:/cart";
    }
}
