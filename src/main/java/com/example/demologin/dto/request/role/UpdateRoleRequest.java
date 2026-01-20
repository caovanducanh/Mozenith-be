package com.example.demologin.dto.request.role;
import com.example.demologin.dto.request.BaseActionRequest;

import com.example.demologin.dto.request.BaseActionRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
    @NotBlank(message = "Role name must not be blank")
    public String name;
    
    // reason field inherited from BaseActionRequest
    // Will be auto-populated when @UserAction(requiresReason = true)
}
