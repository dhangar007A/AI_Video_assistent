package com.aivideoassistant.service.impl;

import com.aivideoassistant.dto.ChatResponse;
import com.aivideoassistant.exception.BadRequestException;
import com.aivideoassistant.exception.ResourceNotFoundException;
import com.aivideoassistant.model.ChatMessage;
import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.repository.MeetingRepository;
import com.aivideoassistant.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MeetingRepository meetingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.python-path:python}")
    private String configuredPythonPath;

    @Value("${app.project-root:..}")
    private String projectRoot;

    @Override
    public ChatResponse askQuestion(String meetingId, String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new BadRequestException("question is required");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!"completed".equalsIgnoreCase(meeting.getStatus())) {
            throw new BadRequestException("Meeting is still processing or failed");
        }

        if (meeting.getChatHistory() == null) {
            meeting.setChatHistory(new ArrayList<>());
        }

        // Add user question to history
        meeting.getChatHistory().add(ChatMessage.builder()
                .role("user")
                .content(question)
                .timestamp(Instant.now())
                .build());

        // Invoke Python RAG chat bridge
        String answer = executeChatBridge(meetingId, question);

        // Add assistant answer to history
        meeting.getChatHistory().add(ChatMessage.builder()
                .role("assistant")
                .content(answer)
                .timestamp(Instant.now())
                .build());

        meetingRepository.save(meeting);

        return ChatResponse.builder()
                .answer(answer)
                .chatHistory(meeting.getChatHistory())
                .build();
    }

    @Override
    public List<ChatMessage> getChatHistory(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        return meeting.getChatHistory() != null ? meeting.getChatHistory() : List.of();
    }

    private String executeChatBridge(String meetingId, String question) {
        Path rootPath = Paths.get(projectRoot).toAbsolutePath().normalize();
        String pythonExe = resolvePythonExecutable(rootPath);
        File scriptFile = rootPath.resolve("chat_bridge.py").toFile();

        if (!scriptFile.exists()) {
            throw new RuntimeException("chat_bridge.py not found at " + scriptFile.getAbsolutePath());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExe,
                scriptFile.getAbsolutePath(),
                "--question", question
        );

        processBuilder.directory(rootPath.toFile());
        Map<String, String> env = processBuilder.environment();
        env.put("MEETING_ID", meetingId);

        StringBuilder stdoutAccumulator = new StringBuilder();
        StringBuilder stderrAccumulator = new StringBuilder();

        try {
            Process process = processBuilder.start();

            // Read stderr
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrAccumulator.append(line).append("\n");
                    }
                } catch (Exception ignored) {
                }
            });
            stderrThread.start();

            // Read stdout
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutAccumulator.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            stderrThread.join(3000);

            if (exitCode != 0) {
                String err = stderrAccumulator.toString().trim();
                log.error("chat_bridge.py failed with exit code {}: {}", exitCode, err);
                throw new RuntimeException(err.isEmpty() ? "Chat bridge exited with code " + exitCode : err);
            }

            // Parse last line of stdout as JSON
            String[] lines = stdoutAccumulator.toString().split("\\r?\\n");
            String lastJsonLine = "";
            for (int i = lines.length - 1; i >= 0; i--) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    lastJsonLine = trimmed;
                    break;
                }
            }

            if (lastJsonLine.isEmpty()) {
                throw new RuntimeException("No valid JSON response from chat bridge: " + stdoutAccumulator);
            }

            JsonNode rootNode = objectMapper.readTree(lastJsonLine);
            if (rootNode.has("error") && !rootNode.get("error").isNull()) {
                throw new RuntimeException(rootNode.get("error").asText());
            }

            if (rootNode.has("answer")) {
                return rootNode.get("answer").asText();
            }

            throw new RuntimeException("Chat bridge response missing 'answer' field: " + lastJsonLine);

        } catch (Exception e) {
            log.error("Failed executing chat bridge for meeting {}", meetingId, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String resolvePythonExecutable(Path rootPath) {
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
