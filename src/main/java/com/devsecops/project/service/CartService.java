package com.devsecops.project.service;

import com.devsecops.project.model.CartItem;
import com.devsecops.project.model.Product;
import com.devsecops.project.model.User;
import com.devsecops.project.repository.CartItemRepository;
import com.devsecops.project.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public List<CartItem> getCart(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    public BigDecimal total(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    /**
     * VULNERABILITY #15: the unit price and the quantity are trusted as sent by the browser
     * (negative quantities or a price of 0.01 are accepted).
     * VULNERABILITY #16: the product id is not validated; Optional.get() throws a raw exception.
     */
    @Transactional
    public void addToCart(User user, Long productId, int quantity, BigDecimal clientPrice) {
        Product product = productRepository.findById(productId).get();
        BigDecimal price = clientPrice != null ? clientPrice : product.getPrice();

        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), productId).orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        item.setUnitPrice(price);
        cartItemRepository.save(item);
    }

    /** VULNERABILITY #10 (part): no check that the cart item belongs to the logged-in user. */
    @Transactional
    public void updateQuantity(Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId).get();
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    /** VULNERABILITY #10 (part): no ownership check either. */
    @Transactional
    public void remove(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }
}
