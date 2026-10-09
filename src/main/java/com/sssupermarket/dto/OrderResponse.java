package com.sssupermarket.dto;

import com.sssupermarket.entity.Order;
import com.sssupermarket.entity.OrderStatus;
import com.sssupermarket.entity.PaymentMethod;
import com.sssupermarket.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponse {

    private Long id;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items = new ArrayList<>();
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String shippingFullName;
    private String shippingMobile;
    private String shippingAddress;

    public OrderResponse() {
    }

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setShippingFullName(order.getShippingFullName());
        response.setShippingMobile(order.getShippingMobile());
        if (order.getShippingAddressLine1() != null) {
            response.setShippingAddress(order.getShippingAddressLine1() + ", " + order.getShippingCity() + ", "
                + order.getShippingState() + " " + order.getShippingPincode());
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
    public PaymentMethod getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(PaymentMethod v){paymentMethod=v;}
    public PaymentStatus getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(PaymentStatus v){paymentStatus=v;}
    public String getShippingFullName(){return shippingFullName;} public void setShippingFullName(String v){shippingFullName=v;}
    public String getShippingMobile(){return shippingMobile;} public void setShippingMobile(String v){shippingMobile=v;}
    public String getShippingAddress(){return shippingAddress;} public void setShippingAddress(String v){shippingAddress=v;}
}
