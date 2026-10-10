package com.sssupermarket.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatResponse {
    private String reply;
    private List<String> suggestions = new ArrayList<>();

    public AiChatResponse() {
    }

    public AiChatResponse(String reply) {
        this.reply = reply;
    }

    public AiChatResponse(String reply, List<String> suggestions) {
        this.reply = reply;
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
