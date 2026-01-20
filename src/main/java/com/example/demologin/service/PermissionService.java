package com.example.demologin.service;

import com.example.demologin.dto.request.PermissionRequest;
import com.example.demologin.dto.response.PermissionResponse;
import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getAll();
    PermissionResponse updatePermissionName(Long id, PermissionRequest req);
} 
