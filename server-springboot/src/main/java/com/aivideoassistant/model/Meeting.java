package com.aivideoassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meetings")
public class Meeting {

    @Id
    private String id;

    @JsonProperty("_id")
    public String getUnderlyingId() {
        return id;
    }

    private String source; // YouTube URL or local file path

    @Builder.Default
    private String language = "english";

    @Builder.Default
    private String status = "processing"; // "processing", "completed", "failed"

    @Builder.Default
    private String currentStage = "downloading";

    @Builder.Default
    private String title = "";

    @Builder.Default
    private String transcript = "";

    @Builder.Default
    private String summary = "";

    @Builder.Default
    private String actionItems = "";

    @Builder.Default
    private String keyDecisions = "";

    @Builder.Default
    private String openQuestions = "";

    @Builder.Default
    private String errorMessage = "";

    @Builder.Default
    private List<ChatMessage> chatHistory = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
