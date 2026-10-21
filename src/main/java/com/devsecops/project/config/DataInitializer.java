package com.devsecops.project.config;

import com.devsecops.project.model.Order;
import com.devsecops.project.model.OrderItem;
import com.devsecops.project.model.Product;
import com.devsecops.project.model.Review;
import com.devsecops.project.model.User;
import com.devsecops.project.repository.OrderRepository;
import com.devsecops.project.repository.ProductRepository;
import com.devsecops.project.repository.ReviewRepository;
import com.devsecops.project.repository.UserRepository;
import com.devsecops.project.service.CryptoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loads fictional sample data when the database is empty.
 * To reset the data: stop the application and delete the "data" folder (see README).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CryptoService cryptoService;

    public DataInitializer(UserRepository userRepository, ProductRepository productRepository,
                           ReviewRepository reviewRepository, OrderRepository orderRepository,
                           CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.cryptoService = cryptoService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // data already loaded
        }

        // VULNERABILITY #3: weak, guessable demonstration credentials.
        User admin = createUser("admin", "admin123", "admin@vulnerableshop.test", "Ada Admin",
                "1 Admin Street, Testville", "ADMIN", "4111111111111111");
        User alice = createUser("alice", "alice123", "alice@vulnerableshop.test", "Alice Martin",
                "12 Rose Avenue, Testville", "USER", "4222222222222222");
        User bob = createUser("bob", "123456", "bob@vulnerableshop.test", "Bob Durand",
                "34 Oak Road, Sampletown", "USER", "4333333333333333");
        createUser("charlie", "qwerty", "charlie@vulnerableshop.test", "Charlie Roy",
                "56 Pine Lane, Demoville", "USER", "4444444444444444");

        Product headphones = createProduct("Wireless Headphones", "Over-ear Bluetooth headphones with 30 hours of battery life.", "59.99", 25, "\uD83C\uDFA7");
        Product watch = createProduct("Smart Watch", "Fitness tracking, notifications and a bright always-on display.", "129.90", 12, "\u231A");
        Product laptop = createProduct("Budget Laptop", "14-inch laptop for study: 8 GB RAM, 256 GB SSD.", "499.00", 6, "\uD83D\uDCBB");
        Product phoneCase = createProduct("Phone Case", "Shock-absorbing case that fits most common phones.", "12.50", 100, "\uD83D\uDCF1");
        createProduct("Gaming Mouse", "Ergonomic mouse with adjustable DPI and programmable buttons.", "34.99", 40, "\uD83D\uDDB1");
        createProduct("Mechanical Keyboard", "Compact keyboard with tactile switches and white backlight.", "79.00", 18, "\u2328");
        createProduct("Laptop Backpack", "Water-resistant backpack with a padded 15-inch laptop sleeve.", "45.00", 30, "\uD83C\uDF92");
        createProduct("USB-C Charger", "30 W fast charger with a foldable plug.", "19.99", 80, "\uD83D\uDD0C");

        createReview(headphones, "alice", "Great sound for the price, battery lasts all week.");
        createReview(headphones, "bob", "Comfortable, but the case could be sturdier.");
        createReview(watch, "alice", "Accurate step counter and a very clear screen.");

        createOrder(alice, "ORD-100001", 3, "DELIVERED", new Product[]{headphones, phoneCase}, new int[]{1, 2});
        createOrder(bob, "ORD-100002", 1, "SHIPPED", new Product[]{laptop}, new int[]{1});
        createOrder(admin, "ORD-100003", 0, "PLACED", new Product[]{watch}, new int[]{1});
    }

    private User createUser(String username, String rawPassword, String email, String fullName,
                            String address, String role, String card) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(cryptoService.md5(rawPassword));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setAddress(address);
        user.setRole(role);
        user.setCardNumber(cryptoService.encrypt(card));
        return userRepository.save(user);
    }

    private Product createProduct(String name, String description, String price, int stock, String icon) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setIcon(icon);
        return productRepository.save(product);
    }

    private void createReview(Product product, String author, String content) {
        Review review = new Review();
        review.setProductId(product.getId());
        review.setAuthor(author);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    private void createOrder(User user, String ref, int daysAgo, String status, Product[] products, int[] quantities) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderRef(ref);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        order.setShippingAddress(user.getAddress());

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < products.length; i++) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(products[i]);
            item.setQuantity(quantities[i]);
            item.setPrice(products[i].getPrice());
            order.getItems().add(item);
            total = total.add(item.getLineTotal());
        }
        order.setTotal(total);
        orderRepository.save(order);
    }
}
