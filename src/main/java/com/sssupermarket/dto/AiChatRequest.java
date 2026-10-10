package com.sssupermarket.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatRequest {
    private String message;
    private Long productId;
    private List<AiChatMessageDto> history = new ArrayList<>();

    public AiChatRequest() {
    }

    public AiChatRequest(String message, Long productId) {
        this.message = message;
        this.productId = productId;
    }

    public AiChatRequest(String message, Long productId, List<AiChatMessageDto> history) {
        this.message = message;
        this.productId = productId;
        this.history = history != null ? history : new ArrayList<>();
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

    public List<AiChatMessageDto> getHistory() {
        return history;
    }

    public void setHistory(List<AiChatMessageDto> history) {
        this.history = history != null ? history : new ArrayList<>();
    }
}
