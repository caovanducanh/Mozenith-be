package com.example.demologin.service;

import com.example.demologin.dto.request.role.CreateRoleRequest;
import com.example.demologin.dto.request.role.UpdateRoleRequest;
import com.example.demologin.dto.request.role.RolePermissionsRequest;
import com.example.demologin.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> getAll();

    RoleResponse create(CreateRoleRequest req);

    RoleResponse update(Long id, UpdateRoleRequest req);

    void delete(Long id);

    RoleResponse updatePermissions(Long id, RolePermissionsRequest req);

    RoleResponse getById(Long id);
}
