package com.example.demologin.dto.request;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class CalendarEventRequest {
    private String summary;
    private String description;
    private String startDateTime; // ISO-8601 string format
    private String endDateTime;   // ISO-8601 string format
    private String timeZone;
    private String location;
    
    // Helper methods to parse dates when needed
    public OffsetDateTime getStartDateTimeParsed() {
        return startDateTime != null ? OffsetDateTime.parse(startDateTime) : null;
    }
    
    public OffsetDateTime getEndDateTimeParsed() {
        return endDateTime != null ? OffsetDateTime.parse(endDateTime) : null;
    }
}
