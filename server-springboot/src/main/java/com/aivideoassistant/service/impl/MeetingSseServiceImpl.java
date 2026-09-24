package com.aivideoassistant.service.impl;

import com.aivideoassistant.model.PipelineEvent;
import com.aivideoassistant.service.MeetingSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class MeetingSseServiceImpl implements MeetingSseService {

    // 30 minutes timeout for pipeline tasks
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> sseClients = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createEmitter(String meetingId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        sseClients.computeIfAbsent(meetingId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("SSE client connected for meeting {}. Active clients: {}", meetingId, sseClients.get(meetingId).size());

        emitter.onCompletion(() -> removeEmitter(meetingId, emitter));
        emitter.onTimeout(() -> removeEmitter(meetingId, emitter));
        emitter.onError((e) -> removeEmitter(meetingId, emitter));

        // Send initial heartbeat comment to establish connection immediately
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE heartbeat for meeting {}", meetingId);
            removeEmitter(meetingId, emitter);
        }

        return emitter;
    }

    @Override
    public void sendProgress(String meetingId, PipelineEvent event) {
        broadcast(meetingId, "progress", event);
    }

    @Override
    public void sendComplete(String meetingId, Map<String, Object> data) {
        broadcast(meetingId, "complete", data);
    }

    @Override
    public void sendError(String meetingId, String errorMessage) {
        broadcast(meetingId, "error", Map.of("message", errorMessage));
    }

    private void broadcast(String meetingId, String eventName, Object data) {
        List<SseEmitter> emitters = sseClients.get(meetingId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                log.debug("SSE send failed for meeting {}, marking emitter for removal", meetingId);
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                sseClients.remove(meetingId);
            }
        }
    }

    private void removeEmitter(String meetingId, SseEmitter emitter) {
        List<SseEmitter> emitters = sseClients.get(meetingId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sseClients.remove(meetingId);
            }
            log.debug("Removed SSE client for meeting {}. Remaining: {}", meetingId, emitters.size());
        }
    }
}
