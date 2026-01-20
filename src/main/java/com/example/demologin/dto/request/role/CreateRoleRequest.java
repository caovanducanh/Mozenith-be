package com.example.demologin.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest  {
    @NotBlank(message = "Role name must not be blank")
    public String name;
    
    // reason field inherited from BaseActionRequest
    // Will be auto-populated when @UserAction(requiresReason = true)
}
