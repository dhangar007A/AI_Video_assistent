package com.aivideoassistant.service;

public interface PipelineExecutionService {
    void runPipelineAsync(String meetingId, String source, String language);
}
