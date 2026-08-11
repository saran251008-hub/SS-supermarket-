package com.sssupermarket.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "com.sssupermarket.controller")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public String handleProductNotFound(ProductNotFoundException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Product not found");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public String handleOrderNotFound(OrderNotFoundException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Order not found");
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(EmptyCartException.class)
    public String handleEmptyCart(EmptyCartException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Your cart is empty");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(InsufficientStockException.class)
    public String handleInsufficientStock(InsufficientStockException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Insufficient stock");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Access denied");
        model.addAttribute("message", "You do not have permission to access this page.");
        return "error/403";
    }

    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthentication(AuthenticationException ex, Model model) {
        model.addAttribute("status", 401);
        model.addAttribute("error", "Authentication required");
        model.addAttribute("message", "Please log in to continue.");
        return "error/403";
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    public String handleBadInput(RuntimeException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Invalid input");
        model.addAttribute("message", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unhandled exception on page request", ex);
        model.addAttribute("status", 500);
        model.addAttribute("error", "Something went wrong");
        model.addAttribute("message", "An unexpected error occurred. Please try again later.");
        return "error/error";
    }
}
