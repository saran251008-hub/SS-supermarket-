package com.sssupermarket.service;

import com.sssupermarket.dto.OrderItemResponse;
import com.sssupermarket.dto.OrderResponse;
import com.sssupermarket.entity.CartItem;
import com.sssupermarket.entity.Order;
import com.sssupermarket.entity.OrderItem;
import com.sssupermarket.entity.OrderStatus;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.User;
import com.sssupermarket.entity.Address;
import com.sssupermarket.entity.Payment;
import com.sssupermarket.entity.PaymentMethod;
import com.sssupermarket.entity.PaymentStatus;
import com.sssupermarket.exception.EmptyCartException;
import com.sssupermarket.exception.InsufficientStockException;
import com.sssupermarket.exception.OrderNotFoundException;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.repository.CartItemRepository;
import com.sssupermarket.repository.OrderRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import com.sssupermarket.repository.AddressRepository;
import com.sssupermarket.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        AddressRepository addressRepository,
                        PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Order checkout(Long userId, Long addressId, PaymentMethod method) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
        Address address = addressRepository.findByIdAndUserId(addressId, userId).orElseThrow(() -> new IllegalArgumentException("Delivery address not found."));
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) throw new EmptyCartException("Your cart is empty. Add products before checkout.");
        Order order = new Order(user, BigDecimal.ZERO, OrderStatus.CONFIRMED);
        order.setPaymentMethod(method); order.setPaymentStatus(method == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
        order.setShippingFullName(address.getFullName()); order.setShippingMobile(address.getMobileNumber());
        order.setShippingAddressLine1(address.getAddressLine1()); order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity()); order.setShippingState(address.getState()); order.setShippingPincode(address.getPincode()); order.setShippingCountry(address.getCountry());
        BigDecimal total=BigDecimal.ZERO;
        for (CartItem item: cartItems) {
            Product product=productRepository.findByIdForUpdate(item.getProduct().getId()).orElseThrow(() -> new ProductNotFoundException("Product is no longer available."));
            if(item.getQuantity()>product.getStock()) throw new InsufficientStockException("Insufficient stock for " + product.getName() + ".");
            product.setStock(product.getStock()-item.getQuantity()); productRepository.save(product);
            OrderItem orderItem=new OrderItem(order,product,item.getQuantity(),product.getPrice()); order.getOrderItems().add(orderItem);
            total=total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotalAmount(total); order=orderRepository.save(order);
        Payment payment=new Payment(); payment.setOrder(order); payment.setAmount(total); payment.setMethod(method); payment.setStatus(order.getPaymentStatus()); payment.setPaymentReference("SS-"+order.getId()+"-"+System.nanoTime()); paymentRepository.save(payment);
        cartItemRepository.deleteByUserId(userId); return order;
    }

    @Transactional
    public Order createOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Your cart is empty. Add products before checking out.");
        }

        Order order = new Order(user, BigDecimal.ZERO, OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product " + item.getProduct().getName() + " is no longer available."));
            if (item.getQuantity() > product.getStock()) {
                throw new InsufficientStockException("Insufficient stock for " + product.getName()
                        + ". Only " + product.getStock() + " unit(s) left.");
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(order, product, item.getQuantity(), product.getPrice());
            order.getOrderItems().add(orderItem);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found."));
        OrderResponse response = OrderResponse.fromEntity(order);
        response.setItems(order.getOrderItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList());
        return response;
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalAmount();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(order -> {
                    OrderResponse response = OrderResponse.fromEntity(order);
                    response.setItems(order.getOrderItems().stream()
                            .map(OrderItemResponse::fromEntity)
                            .toList());
                    return response;
                })
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id " + orderId));
        order.setStatus(status);
        if (status == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
        }
        Order saved = orderRepository.save(order);
        OrderResponse response = OrderResponse.fromEntity(saved);
        response.setItems(saved.getOrderItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList());
        return response;
    }
}
