package com.devsecops.project.service;

import com.devsecops.project.model.Product;
import com.devsecops.project.model.Review;
import com.devsecops.project.repository.ProductRepository;
import com.devsecops.project.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductService(ProductRepository productRepository, ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No product with id " + id));
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public long count() {
        return productRepository.count();
    }

    /**
     * VULNERABILITY #1: SQL injection. The keyword is concatenated directly into a native SQL query.
     * VULNERABILITY #2: SQL injection in ORDER BY. The "sort" value also comes straight from the request.
     * A secure version would use a parameterized query and an allow-list for the sort column.
     */
    @SuppressWarnings("unchecked")
    public List<Product> search(String keyword, String sort) {
        String sql = "SELECT * FROM products WHERE name LIKE '%" + keyword + "%' "
                + "OR description LIKE '%" + keyword + "%' ORDER BY " + sort;
        return entityManager.createNativeQuery(sql, Product.class).getResultList();
    }

    public List<Review> getReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /** VULNERABILITY #13 (storage side): review text is saved without any sanitization. */
    public Review addReview(Long productId, String author, String content) {
        Review review = new Review();
        review.setProductId(productId);
        review.setAuthor(author);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }
}
