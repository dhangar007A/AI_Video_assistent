package com.aivideoassistant.dto;

import com.aivideoassistant.model.Meeting;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryResponse {

    private String id;

    @JsonProperty("_id")
    public String getUnderlyingId() {
        return id;
    }

    private String source;
    private String language;
    private String status;
    private String currentStage;
    private String title;
    private String summary;
    private String actionItems;
    private String keyDecisions;
    private String openQuestions;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public static MeetingSummaryResponse fromMeeting(Meeting meeting) {
        return MeetingSummaryResponse.builder()
                .id(meeting.getId())
                .source(meeting.getSource())
                .language(meeting.getLanguage())
                .status(meeting.getStatus())
                .currentStage(meeting.getCurrentStage())
                .title(meeting.getTitle())
                .summary(meeting.getSummary())
                .actionItems(meeting.getActionItems())
                .keyDecisions(meeting.getKeyDecisions())
                .openQuestions(meeting.getOpenQuestions())
                .errorMessage(meeting.getErrorMessage())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }
}
