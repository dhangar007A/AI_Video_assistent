package com.aivideoassistant.service;

import com.aivideoassistant.model.PipelineEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface MeetingSseService {
    SseEmitter createEmitter(String meetingId);
    void sendProgress(String meetingId, PipelineEvent event);
    void sendComplete(String meetingId, Map<String, Object> data);
    void sendError(String meetingId, String errorMessage);
}
