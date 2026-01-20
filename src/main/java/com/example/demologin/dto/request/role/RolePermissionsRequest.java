package com.example.demologin.dto.request.role;
import com.example.demologin.dto.request.BaseActionRequest;

import com.example.demologin.dto.request.BaseActionRequest;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RolePermissionsRequest {
    @NotEmpty(message = "Permission IDs must not be empty")
    public Set<Long> permissionIds;
    
    // reason field inherited from BaseActionRequest
    // Will be auto-populated when @UserAction(requiresReason = true)
}
