package com.sssupermarket;

import com.sssupermarket.dto.AiChatMessageDto;
import com.sssupermarket.dto.AiChatRequest;
import com.sssupermarket.dto.AiChatResponse;
import com.sssupermarket.entity.Product;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.service.AiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AiChatServiceTest {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testNavigationGuide_ReturnsCompleteTour() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("how to navigate the whole webpage");

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("SS Supermarket Complete Webpage Navigation Guide");
        assertThat(response.getReply()).contains("/products");
        assertThat(response.getReply()).contains("/cart");
        assertThat(response.getReply()).contains("/checkout");
        assertThat(response.getReply()).contains("/orders");
        assertThat(response.getReply()).contains("/seller");
        assertThat(response.getReply()).contains("/admin");
    }

    @Test
    void testExplainProduct_HowToUseDirect() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("how to use laptop");

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("Laptop");
        assertThat(response.getReply()).contains("How to Use & Best Practices");
        assertThat(response.getReply()).contains("Unbox & Initial Charge");
        assertThat(response.getReply()).contains("Rating");
        assertThat(response.getReply()).contains("Price");
    }

    @Test
    void testContextualProductWithId() {
        Product p = productRepository.findAll().stream()
                .filter(item -> item.getName().equalsIgnoreCase("Laptop"))
                .findFirst()
                .orElseThrow();

        AiChatRequest request = new AiChatRequest();
        request.setProductId(p.getId());
        request.setMessage("how to use this product?");

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("Laptop");
        assertThat(response.getReply()).contains("How to Use & Best Practices");
    }

    @Test
    void testChatHistoryContextResolution() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("how to use it?");
        request.setHistory(List.of(
                new AiChatMessageDto("user", "Tell me about boAt Headphones"),
                new AiChatMessageDto("bot", "🛍️ **Headphones**\n\nWireless over-ear headphones")
        ));

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("Headphones");
        assertThat(response.getReply()).contains("How to Use & Best Practices");
        assertThat(response.getReply()).contains("Connectivity");
    }

    @Test
    void testSummarizeChatHistory() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("what did we discuss in our chat history?");
        request.setHistory(List.of(
                new AiChatMessageDto("user", "What are the top rated products?"),
                new AiChatMessageDto("bot", "Top rated products include Laptop and Smartphone.")
        ));

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("Recent Chat History Summary");
        assertThat(response.getReply()).contains("Top rated products");
    }

    @Test
    void testGreetingMessage() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("Hello");

        AiChatResponse response = aiChatService.processMessage(request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("Welcome to SS Supermarket");
        assertThat(response.getSuggestions()).contains("🧭 Navigate website");
    }
}
