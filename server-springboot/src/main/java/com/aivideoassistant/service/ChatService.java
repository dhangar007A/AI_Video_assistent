package com.aivideoassistant.service;

import com.aivideoassistant.dto.ChatResponse;
import com.aivideoassistant.model.ChatMessage;

import java.util.List;

public interface ChatService {
    ChatResponse askQuestion(String meetingId, String question);
    List<ChatMessage> getChatHistory(String meetingId);
}
