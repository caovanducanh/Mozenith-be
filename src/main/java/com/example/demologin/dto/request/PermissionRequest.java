package com.example.demologin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionRequest  {
    @NotBlank(message = "Permission name must not be blank")
    private String name;
    
    // reason field inherited from BaseActionRequest
    // Will be auto-populated when @UserAction(requiresReason = true)
}
