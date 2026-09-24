package com.aivideoassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineEvent {

    private String event; // "progress", "result", "error"

    private String stage;
    private String message;
    private Boolean done;

    // Fields present on "result" event
    private String title;
    private String transcript;
    private String summary;

    @JsonProperty("action_items")
    private String actionItems;

    @JsonProperty("key_decisions")
    private String keyDecisions;

    @JsonProperty("open_questions")
    private String openQuestions;

    // Fields present on "error" event
    private String traceback;
}
