package com.example.demologin.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseActionRequest {
    private String reason;
    
    @JsonIgnore
    public boolean hasReason() {
        return reason != null && !reason.trim().isEmpty();
    }
    
    @JsonIgnore
    public String getReasonOrDefault(String defaultReason) {
        return hasReason() ? reason.trim() : defaultReason;
    }
}
