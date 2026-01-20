package com.example.demologin.mapper;

import com.example.demologin.dto.request.role.CreateRoleRequest;
import com.example.demologin.dto.request.role.RolePermissionsRequest;
import com.example.demologin.dto.request.role.UpdateRoleRequest;
import com.example.demologin.dto.response.PermissionResponse;
import com.example.demologin.dto.response.RoleResponse;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleMapper {
    @Autowired private PermissionRepository permissionRepository;

    public void fromCreateDto(CreateRoleRequest dto, Role entity) {
        entity.setName(dto.name);
    }


    public void fromUpdateDto(UpdateRoleRequest dto, Role entity) {
        entity.setName(dto.name);
    }

    public void fromPermissionDto(RolePermissionsRequest dto, Role entity) {
        if (dto.permissionIds != null) {
            entity.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.permissionIds)));
        }
    }

    public RoleResponse toResponse(Role entity) {
        Set<PermissionResponse> permissions = entity.getPermissions().stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getCode(),
                        permission.getName()
                ))
                .collect(Collectors.toSet());

        return new RoleResponse(entity.getId(), entity.getName(), permissions);
    }

    public List<RoleResponse> toResponseList(List<Role> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
