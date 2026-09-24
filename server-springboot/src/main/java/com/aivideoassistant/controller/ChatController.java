package com.aivideoassistant.controller;

import com.aivideoassistant.dto.ChatRequest;
import com.aivideoassistant.dto.ChatResponse;
import com.aivideoassistant.model.ChatMessage;
import com.aivideoassistant.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints for conversational Q&A on meeting transcripts using RAG")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Ask a question about the meeting using the isolated RAG vector index")
    @PostMapping("/{meetingId}")
    public ChatResponse askQuestion(
            @PathVariable String meetingId,
            @Valid @RequestBody ChatRequest request) {
        log.info("Chat question received for meeting {}: {}", meetingId, request.getQuestion());
        return chatService.askQuestion(meetingId, request.getQuestion());
    }

    @Operation(summary = "Retrieve entire conversational history for a meeting")
    @GetMapping("/{meetingId}/history")
    public List<ChatMessage> getChatHistory(@PathVariable String meetingId) {
        log.info("Retrieving chat history for meeting {}", meetingId);
        return chatService.getChatHistory(meetingId);
    }
}
