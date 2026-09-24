package com.aivideoassistant.service.impl;

import com.aivideoassistant.dto.CreateMeetingRequest;
import com.aivideoassistant.dto.MeetingSummaryResponse;
import com.aivideoassistant.dto.StartPipelineResponse;
import com.aivideoassistant.exception.BadRequestException;
import com.aivideoassistant.exception.ResourceNotFoundException;
import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.repository.MeetingRepository;
import com.aivideoassistant.service.FileStorageService;
import com.aivideoassistant.service.MeetingService;
import com.aivideoassistant.service.PipelineExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final FileStorageService fileStorageService;
    private final PipelineExecutionService pipelineExecutionService;

    @Value("${app.project-root:..}")
    private String projectRoot;

    @Override
    public StartPipelineResponse createMeetingFromUrl(CreateMeetingRequest request) {
        if (request == null || request.getSource() == null || request.getSource().trim().isEmpty()) {
            throw new BadRequestException("Provide a YouTube URL or upload a file");
        }

        String language = (request.getLanguage() != null && !request.getLanguage().trim().isEmpty())
                ? request.getLanguage().trim()
                : "english";

        Meeting meeting = Meeting.builder()
                .source(request.getSource().trim())
                .language(language)
                .status("processing")
                .currentStage("downloading")
                .build();

        Meeting saved = meetingRepository.save(meeting);
        log.info("Created meeting record with id: {}", saved.getId());

        // Asynchronously run pipeline
        pipelineExecutionService.runPipelineAsync(saved.getId(), saved.getSource(), saved.getLanguage());

        return StartPipelineResponse.builder()
                .meetingId(saved.getId())
                .status("processing")
                .message("Pipeline started. Connect to SSE for progress.")
                .build();
    }

    @Override
    public StartPipelineResponse createMeetingFromFile(MultipartFile file, String language) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Provide a YouTube URL or upload a file");
        }

        String lang = (language != null && !language.trim().isEmpty()) ? language.trim() : "english";
        String storedFilePath = fileStorageService.storeFile(file);

        Meeting meeting = Meeting.builder()
                .source(storedFilePath)
                .language(lang)
                .status("processing")
                .currentStage("downloading")
                .build();

        Meeting saved = meetingRepository.save(meeting);
        log.info("Created meeting record from file with id: {}", saved.getId());

        // Asynchronously run pipeline
        pipelineExecutionService.runPipelineAsync(saved.getId(), saved.getSource(), saved.getLanguage());

        return StartPipelineResponse.builder()
                .meetingId(saved.getId())
                .status("processing")
                .message("Pipeline started. Connect to SSE for progress.")
                .build();
    }

    @Override
    public List<MeetingSummaryResponse> getAllMeetings() {
        return meetingRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(50)
                .map(MeetingSummaryResponse::fromMeeting)
                .toList();
    }

    @Override
    public Meeting getMeetingById(String id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    @Override
    public void deleteMeeting(String id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meeting not found");
        }

        meetingRepository.deleteById(id);
        log.info("Deleted meeting {} from database", id);

        // Clean up Chroma vector database directory for this meeting
        cleanupVectorDb(id);
    }

    private void cleanupVectorDb(String meetingId) {
        try {
            Path rootPath = Paths.get(projectRoot).toAbsolutePath().normalize();
            Path vectorDbPath = rootPath.resolve("vector_db_" + meetingId);
            File vectorDbDir = vectorDbPath.toFile();

            if (vectorDbDir.exists() && vectorDbDir.isDirectory()) {
                log.info("Cleaning up vector DB directory at: {}", vectorDbDir.getAbsolutePath());
                try (var stream = Files.walk(vectorDbPath)) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to delete vector DB directory for meeting {}: {}", meetingId, e.getMessage());
        }
    }
}
