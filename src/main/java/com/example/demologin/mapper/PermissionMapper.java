package com.example.demologin.mapper;

import com.example.demologin.dto.request.PermissionRequest;
import com.example.demologin.dto.response.PermissionResponse;
import com.example.demologin.entity.Permission;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PermissionMapper {
    public void updateEntityFromDto(PermissionRequest dto, Permission entity) {
        entity.setName(dto.getName());
    }

    public PermissionResponse toResponse(Permission entity) {
        return new PermissionResponse(entity.getId(), entity.getCode(), entity.getName());
    }

    public List<PermissionResponse> toResponseList(List<Permission> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
