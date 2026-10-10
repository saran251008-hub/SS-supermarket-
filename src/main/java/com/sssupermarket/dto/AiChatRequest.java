package com.sssupermarket.dto;

public class AiChatRequest {
    private String message;
    private Long productId;

    public AiChatRequest() {
    }

    public AiChatRequest(String message, Long productId) {
        this.message = message;
        this.productId = productId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
