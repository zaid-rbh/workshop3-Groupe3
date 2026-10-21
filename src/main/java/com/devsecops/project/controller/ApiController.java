package com.devsecops.project.controller;

import com.devsecops.project.model.Product;
import com.devsecops.project.model.User;
import com.devsecops.project.service.ProductService;
import com.devsecops.project.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Small JSON API.
 * VULNERABILITY #23: no authentication, CORS open to every origin (see WebConfig), and
 * /api/users/{id} returns the complete User entity (password hash and encrypted card included).
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ProductService productService;
    private final UserService userService;

    public ApiController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping("/products")
    public List<Product> products() {
        return productService.findAll();
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping("/users/{id}")
    public User user(@PathVariable Long id) {
        return userService.getById(id);
    }
}
