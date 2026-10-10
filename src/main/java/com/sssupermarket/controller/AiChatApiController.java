package com.sssupermarket.controller;

import com.sssupermarket.dto.AiChatRequest;
import com.sssupermarket.dto.AiChatResponse;
import com.sssupermarket.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatApiController {

    private final AiChatService aiChatService;

    public AiChatApiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody(required = false) AiChatRequest request) {
        if (request == null) {
            request = new AiChatRequest();
        }
        AiChatResponse response = aiChatService.processMessage(request);
        return ResponseEntity.ok(response);
    }
}
