package com.aivideoassistant.service.impl;

import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.model.PipelineEvent;
import com.aivideoassistant.repository.MeetingRepository;
import com.aivideoassistant.service.MeetingSseService;
import com.aivideoassistant.service.PipelineExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineExecutionServiceImpl implements PipelineExecutionService {

    private final MeetingRepository meetingRepository;
    private final MeetingSseService meetingSseService;
    private final ObjectMapper objectMapper;

    @Value("${app.python-path:python}")
    private String configuredPythonPath;

    @Value("${app.project-root:..}")
    private String projectRoot;

    @Override
    @Async("pipelineTaskExecutor")
    public void runPipelineAsync(String meetingId, String source, String language) {
        log.info("Starting background pipeline for meeting {} with source '{}' and language '{}'", meetingId, source, language);

        Path rootPath = Paths.get(projectRoot).toAbsolutePath().normalize();
        String pythonExe = resolvePythonExecutable(rootPath);
        File scriptFile = rootPath.resolve("api_bridge.py").toFile();

        if (!scriptFile.exists()) {
            String errorMsg = "api_bridge.py not found at " + scriptFile.getAbsolutePath();
            log.error(errorMsg);
            markMeetingFailed(meetingId, errorMsg);
            meetingSseService.sendError(meetingId, errorMsg);
            return;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExe,
                scriptFile.getAbsolutePath(),
                "--source", source,
                "--language", language != null ? language : "english"
        );

        processBuilder.directory(rootPath.toFile());
        Map<String, String> env = processBuilder.environment();
        env.put("MEETING_ID", meetingId);

        StringBuilder stderrAccumulator = new StringBuilder();

        try {
            Process process = processBuilder.start();

            // Thread for reading stderr in real time
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrAccumulator.append(line).append("\n");
                        log.debug("[Python STDERR] {}", line);
                    }
                } catch (Exception e) {
                    log.warn("Error reading Python stderr for meeting {}", meetingId, e);
                }
            });
            stderrThread.start();

            // Read stdout line-by-line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    try {
                        PipelineEvent event = objectMapper.readValue(line, PipelineEvent.class);
                        if (event.getEvent() != null) {
                            handlePipelineEvent(meetingId, event);
                        }
                    } catch (Exception e) {
                        // Non-JSON stdout line (e.g. logging output from libraries), safely ignore
                        log.debug("[Python STDOUT non-JSON] {}", line);
                    }
                }
            }

            int exitCode = process.waitFor();
            stderrThread.join(5000);

            if (exitCode != 0) {
                String errorDetails = stderrAccumulator.toString().trim();
                String message = "Python pipeline process exited with code " + exitCode +
                        (errorDetails.isEmpty() ? "" : ": " + errorDetails);
                log.error("Pipeline failed for meeting {}: {}", meetingId, message);
                markMeetingFailed(meetingId, message);
                meetingSseService.sendError(meetingId, message);
            }

        } catch (Exception e) {
            log.error("Exception executing pipeline process for meeting {}", meetingId, e);
            markMeetingFailed(meetingId, e.getMessage());
            meetingSseService.sendError(meetingId, e.getMessage());
        }
    }

    private void handlePipelineEvent(String meetingId, PipelineEvent event) {
        switch (event.getEvent()) {
            case "progress" -> {
                meetingRepository.findById(meetingId).ifPresent(m -> {
                    m.setCurrentStage(event.getStage() != null ? event.getStage() : m.getCurrentStage());
                    meetingRepository.save(m);
                });
                meetingSseService.sendProgress(meetingId, event);
            }
            case "result" -> {
                meetingRepository.findById(meetingId).ifPresent(m -> {
                    m.setStatus("completed");
                    m.setCurrentStage("done");
                    m.setTitle(event.getTitle() != null ? event.getTitle() : "");
                    m.setTranscript(event.getTranscript() != null ? event.getTranscript() : "");
                    m.setSummary(event.getSummary() != null ? event.getSummary() : "");
                    m.setActionItems(event.getActionItems() != null ? event.getActionItems() : "");
                    m.setKeyDecisions(event.getKeyDecisions() != null ? event.getKeyDecisions() : "");
                    m.setOpenQuestions(event.getOpenQuestions() != null ? event.getOpenQuestions() : "");
                    meetingRepository.save(m);
                });
                meetingSseService.sendComplete(meetingId, Map.of("meetingId", meetingId));
                log.info("Pipeline completed successfully for meeting {}", meetingId);
            }
            case "error" -> {
                String errorMsg = event.getMessage() != null ? event.getMessage() : "Unknown error in Python pipeline";
                markMeetingFailed(meetingId, errorMsg);
                meetingSseService.sendError(meetingId, errorMsg);
            }
            default -> log.debug("Unknown pipeline event type: {}", event.getEvent());
        }
    }

    private void markMeetingFailed(String meetingId, String errorMessage) {
        meetingRepository.findById(meetingId).ifPresent(m -> {
            m.setStatus("failed");
            m.setErrorMessage(errorMessage);
            meetingRepository.save(m);
        });
    }

    private String resolvePythonExecutable(Path rootPath) {
        // Check for virtual environment in project root
        File venvWin = rootPath.resolve(".venv").resolve("Scripts").resolve("python.exe").toFile();
        if (venvWin.exists()) {
            return venvWin.getAbsolutePath();
        }
        File venvUnix = rootPath.resolve(".venv").resolve("bin").resolve("python").toFile();
        if (venvUnix.exists()) {
            return venvUnix.getAbsolutePath();
        }
        return configuredPythonPath;
    }
}
