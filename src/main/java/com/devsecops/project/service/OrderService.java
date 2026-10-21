package com.devsecops.project.service;

import com.devsecops.project.dto.CheckoutForm;
import com.devsecops.project.model.CartItem;
import com.devsecops.project.model.Order;
import com.devsecops.project.model.OrderItem;
import com.devsecops.project.model.User;
import com.devsecops.project.repository.CartItemRepository;
import com.devsecops.project.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public Order placeOrder(User user, CheckoutForm form) {
        List<CartItem> cart = cartItemRepository.findByUserId(user.getId());

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PLACED");
        order.setShippingAddress(form.getShippingAddress());

        // VULNERABILITY #19 (part): java.util.Random is predictable; it is used to build the order reference.
        order.setOrderRef("ORD-" + (100000 + new Random().nextInt(900000)));

        BigDecimal computedTotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getUnitPrice());
            order.getItems().add(item);
            computedTotal = computedTotal.add(item.getLineTotal());
        }

        // VULNERABILITY #15: the server prefers the total sent by the browser over its own computation.
        order.setTotal(form.getTotal() != null ? form.getTotal() : computedTotal);

        // VULNERABILITY #20: the (fictional) card number is written to the log in clear text.
        log.info("Order placed by {} - card={} - total={}", user.getUsername(), form.getCardNumber(), order.getTotal());

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(user.getId());
        return saved;
    }

    public List<Order> findForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No order with id " + id));
    }

    public long count() {
        return orderRepository.count();
    }

    public BigDecimal revenue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Order order : orderRepository.findAll()) {
            if (order.getTotal() != null) {
                sum = sum.add(order.getTotal());
            }
        }
        return sum;
    }
}
