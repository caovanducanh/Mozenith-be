package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.annotation.SmartCache;
import com.example.demologin.dto.request.role.CreateRoleRequest;
import com.example.demologin.dto.request.role.RolePermissionsRequest;
import com.example.demologin.dto.request.role.UpdateRoleRequest;
import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/roles")
@Tag(name = "Role Management", description = "APIs for managing user roles and role permissions")
    public class RoleController {
    private final RoleService roleService;

    @SecuredEndpoint("ROLE_VIEW")
    @GetMapping
    @SmartCache
    @ApiResponse(message = "Roles retrieved successfully")
    @Operation(summary = "Get all roles", 
               description = "Retrieve all roles in the system")
    public Object getAll() {
        return roleService.getAll();
    }

    @PostMapping
    @ApiResponse(message = "Role created successfully")
    @SecuredEndpoint("ROLE_CREATE")
    @SmartCache
    @Operation(summary = "Create new role", 
               description = "Create a new role with specified name and description")
    public Object create(@RequestBody @Valid CreateRoleRequest req) {
        return roleService.create(req);
    }

    @PutMapping("/{id}")
    @ApiResponse(message = "Role updated successfully")
    @SmartCache
    @SecuredEndpoint("ROLE_UPDATE")
    @Operation(summary = "Update role", 
               description = "Update role name and description")
    public Object update(
            @Parameter(description = "Role ID") @PathVariable Long id, 
            @RequestBody @Valid UpdateRoleRequest req) {
        return roleService.update(id, req);
    }

    @DeleteMapping("/{id}")
//    @ApiResponse(message = "Role deleted successfully")
    @SmartCache
    @SecuredEndpoint("ROLE_DELETE")
    @Operation(summary = "Delete role", 
               description = "Delete a role from the system")
    public ResponseObject delete(@PathVariable Long id) {
        roleService.delete(id);
        return new ResponseObject(HttpStatus.OK.value(), "Role deleted successfully", null);
    }

    @PutMapping("/{id}/permissions")
    @ApiResponse(message = "Role permissions updated successfully")
    @SecuredEndpoint("ROLE_UPDATE_PERMISSIONS")
    @SmartCache
    @Operation(summary = "Update role permissions", 
               description = "Update permissions assigned to a role")
    public Object updatePermissions(
            @Parameter(description = "Role ID") @PathVariable Long id, 
            @RequestBody @Valid RolePermissionsRequest req) {
        return roleService.updatePermissions(id, req);
    }

    @SecuredEndpoint("ROLE_VIEW")
    @GetMapping("/{id}")
    @SmartCache
    @ApiResponse(message = "Role retrieved successfully")
    @Operation(summary = "Get role by ID",
            description = "Retrieve a role by its ID")
    public Object getById(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        return roleService.getById(id);
    }

}
