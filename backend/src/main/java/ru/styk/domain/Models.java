package ru.styk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class Models {
    private Models() {}
    @MappedSuperclass public abstract static class EntityId {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    }
    @Entity(name="Account") @Table(name="users") public static class Account extends EntityId {
        public String email, name;
        @JsonIgnore public String password;
        public String role;
        public boolean enabled = true;
    }
    @Entity(name="Product") @Table(name="products") public static class Product extends EntityId {
        public Long sellerId;
        public String name, brand, category;
        @Column(length=4000) public String description;
        @Column(precision=12,scale=2) public BigDecimal price;
        @Column(precision=12,scale=2) public BigDecimal oldPrice;
        public String image, color, sku, status;
        public int stock, reserved, threshold = 10, target = 40;
        public String location = "A-01";
        public int getAvailable() { return stock - reserved; }
    }
    @Entity(name="CartItem") @Table(name="cart_items") public static class CartItem extends EntityId {
        public Long userId, productId; public int quantity;
    }
    @Entity(name="Purchase") @Table(name="orders") public static class Purchase extends EntityId {
        public Long userId; public String status, paymentStatus, address, delivery, requestKey;
        @Column(precision=12,scale=2) public BigDecimal total, discount;
        public Instant createdAt = Instant.now();
    }
    @Entity(name="OrderItem") @Table(name="order_items") public static class OrderItem extends EntityId {
        public Long orderId, productId, sellerId; public String name;
        @Column(precision=12,scale=2) public BigDecimal price;
        public int quantity;
    }
    @Entity(name="Supply") @Table(name="supply_requests") public static class Supply extends EntityId {
        public Long productId, sellerId; public int quantity, received;
        public String status, note = ""; public Instant createdAt = Instant.now();
    }
    @Entity(name="Movement") @Table(name="stock_movements") public static class Movement extends EntityId {
        public Long productId; public String type, reference; public int quantity;
        public Instant createdAt = Instant.now();
    }
    @Entity(name="OrderEvent") @Table(name="order_events") public static class OrderEvent extends EntityId {
        public Long orderId; public String status, note; public Instant createdAt = Instant.now();
    }
    @Entity(name="Review") @Table(name="reviews") public static class Review extends EntityId {
        public Long userId, productId; public String author, text; public int rating;
        public Instant createdAt = Instant.now();
    }
    @Entity(name="Favorite") @Table(name="favorites") public static class Favorite extends EntityId {
        public Long userId, productId;
    }
    @Entity(name="Audit") @Table(name="audit_events") public static class Audit extends EntityId {
        public String actor, action; public Instant createdAt = Instant.now();
    }
}
