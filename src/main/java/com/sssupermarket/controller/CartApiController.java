package com.sssupermarket.controller;

import com.sssupermarket.dto.AddToCartRequest;
import com.sssupermarket.dto.ApiResponse;
import com.sssupermarket.dto.CartResponse;
import com.sssupermarket.dto.UpdateCartRequest;
import com.sssupermarket.security.CurrentUser;
import com.sssupermarket.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartApiController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(currentUser.getId(), request.getProductId(), request.getQuantity()));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long itemId,
                                                   @Valid @RequestBody UpdateCartRequest request) {
        return ResponseEntity.ok(cartService.updateQuantity(currentUser.getId(), itemId, request.getQuantity()));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(currentUser.getId(), itemId);
        return ResponseEntity.ok(new ApiResponse("Item removed from cart."));
    }
}
