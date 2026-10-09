package com.sssupermarket.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", unique = true) private Order order;
    @Column(nullable = false, unique = true, length = 80) private String paymentReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist protected void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; } public void setOrder(Order v) { order = v; }
    public String getPaymentReference() { return paymentReference; } public void setPaymentReference(String v) { paymentReference = v; }
    public PaymentMethod getMethod() { return method; } public void setMethod(PaymentMethod v) { method = v; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
    public PaymentStatus getStatus() { return status; } public void setStatus(PaymentStatus v) { status = v; }
}
