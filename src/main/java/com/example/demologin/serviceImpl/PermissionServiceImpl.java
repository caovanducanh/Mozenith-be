package com.example.demologin.serviceImpl;

import com.example.demologin.entity.Permission;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.PermissionRepository;
import com.example.demologin.dto.request.PermissionRequest;
import com.example.demologin.dto.response.PermissionResponse;
import com.example.demologin.mapper.PermissionMapper;
import com.example.demologin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponse> getAll() {
        List<Permission> permissions = permissionRepository.findAll();
        if (permissions.isEmpty()) {
            throw new NotFoundException("No permissions found");
        }
        return permissionMapper.toResponseList(permissions);
    }

    @Override
    public PermissionResponse updatePermissionName(Long id, PermissionRequest req) {
        Permission p = permissionRepository.findById(id).orElseThrow(() -> new NotFoundException("Permission with id " + id + " not found"));
        permissionMapper.updateEntityFromDto(req, p);
        Permission updatedPermission = permissionRepository.save(p);
        return permissionMapper.toResponse(updatedPermission);
    }
} 
