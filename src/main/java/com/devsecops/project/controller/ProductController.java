package com.devsecops.project.controller;

import com.devsecops.project.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * VULNERABILITY #1 and #2: "q" and "sort" go straight to ProductService.search(), which builds SQL by
     * string concatenation.
     * VULNERABILITY #14: "q" is sent back to the page and displayed without escaping (see products.html).
     */
    @GetMapping("/products")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "name") String sort,
                       Model model) {
        model.addAttribute("products", productService.search(q, sort));
        model.addAttribute("query", q);
        model.addAttribute("sort", sort);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getById(id));
        model.addAttribute("reviews", productService.getReviews(id));
        return "product-detail";
    }

    /** VULNERABILITY #13: review text and author name are stored exactly as typed. */
    @PostMapping("/products/{id}/reviews")
    public String addReview(@PathVariable Long id,
                            @RequestParam String author,
                            @RequestParam String content,
                            RedirectAttributes redirectAttributes) {
        productService.addReview(id, author, content);
        redirectAttributes.addFlashAttribute("message", "Thanks, your review has been published.");
        return "redirect:/products/" + id;
    }
}
