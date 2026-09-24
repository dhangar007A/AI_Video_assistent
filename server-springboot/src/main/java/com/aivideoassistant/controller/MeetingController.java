package com.aivideoassistant.controller;

import com.aivideoassistant.dto.CreateMeetingRequest;
import com.aivideoassistant.dto.MeetingSummaryResponse;
import com.aivideoassistant.dto.StartPipelineResponse;
import com.aivideoassistant.exception.BadRequestException;
import com.aivideoassistant.model.Meeting;
import com.aivideoassistant.service.MeetingService;
import com.aivideoassistant.service.MeetingSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "API endpoints for creating meetings, streaming AI progress, and managing meeting records")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingSseService meetingSseService;

    @Operation(summary = "Start processing a meeting from a YouTube URL (JSON)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StartPipelineResponse createMeetingJson(@RequestBody CreateMeetingRequest request) {
        log.info("Received JSON create meeting request for source: {}", request.getSource());
        return meetingService.createMeetingFromUrl(request);
    }

    @Operation(summary = "Start processing a meeting from an uploaded media file or form data")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StartPipelineResponse createMeetingMultipart(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "language", defaultValue = "english") String language) {
        if (file != null && !file.isEmpty()) {
            log.info("Received file upload: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
            return meetingService.createMeetingFromFile(file, language);
        } else if (source != null && !source.trim().isEmpty()) {
            log.info("Received multipart form-data source URL: {}", source);
            return meetingService.createMeetingFromUrl(new CreateMeetingRequest(source, language));
        } else {
            throw new BadRequestException("Provide a YouTube URL or upload a file");
        }
    }

    @Operation(summary = "List all analyzed meetings (lightweight projection, sorted descending)")
    @GetMapping
    public List<MeetingSummaryResponse> getAllMeetings() {
        return meetingService.getAllMeetings();
    }

    @Operation(summary = "Get full details of a specific meeting including transcript and chat history")
    @GetMapping("/{id}")
    public Meeting getMeetingById(@PathVariable String id) {
        return meetingService.getMeetingById(id);
    }

    @Operation(summary = "Connect to Server-Sent Events (SSE) stream for real-time AI pipeline stage updates")
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMeetingProgress(@PathVariable String id) {
        log.info("Client subscribing to SSE stream for meeting id: {}", id);
        return meetingSseService.createEmitter(id);
    }

    @Operation(summary = "Delete meeting record and associated Chroma vector database from disk")
    @DeleteMapping("/{id}")
    public Map<String, String> deleteMeeting(@PathVariable String id) {
        log.info("Deleting meeting id: {}", id);
        meetingService.deleteMeeting(id);
        return Map.of("message", "Meeting deleted");
    }
}
