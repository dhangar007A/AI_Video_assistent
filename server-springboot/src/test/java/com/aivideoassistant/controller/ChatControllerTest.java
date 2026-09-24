package com.aivideoassistant.controller;

import com.aivideoassistant.dto.ChatRequest;
import com.aivideoassistant.dto.ChatResponse;
import com.aivideoassistant.model.ChatMessage;
import com.aivideoassistant.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    @DisplayName("POST /api/chat/{meetingId}: returns 200 OK with answer and history")
    void askQuestion_ReturnsOk() throws Exception {
        ChatRequest request = new ChatRequest("What is the release date?");
        ChatMessage msg1 = new ChatMessage("user", "What is the release date?", Instant.now());
        ChatMessage msg2 = new ChatMessage("assistant", "The release date is next Friday.", Instant.now());

        ChatResponse response = ChatResponse.builder()
                .answer("The release date is next Friday.")
                .chatHistory(List.of(msg1, msg2))
                .build();

        when(chatService.askQuestion(eq("meet-1"), eq("What is the release date?"))).thenReturn(response);

        mockMvc.perform(post("/api/chat/meet-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The release date is next Friday."))
                .andExpect(jsonPath("$.chatHistory.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/chat/{meetingId}: returns 400 Bad Request on blank question")
    void askQuestion_BlankQuestion_ReturnsBadRequest() throws Exception {
        ChatRequest request = new ChatRequest("");

        mockMvc.perform(post("/api/chat/meet-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/chat/{meetingId}/history: returns chat history list")
    void getChatHistory_ReturnsHistoryList() throws Exception {
        ChatMessage msg = new ChatMessage("user", "Hello", Instant.now());
        when(chatService.getChatHistory("meet-1")).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/chat/meet-1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello"));
    }
}
