package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.role.CreateRoleRequest;
import com.example.demologin.dto.request.role.RolePermissionsRequest;
import com.example.demologin.dto.request.role.UpdateRoleRequest;
import com.example.demologin.dto.response.RoleResponse;
import com.example.demologin.entity.Role;
import com.example.demologin.exception.exceptions.BusinessException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.RoleMapper;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserRepository userRepository;

    @Override
    public List<RoleResponse> getAll() {
        List<Role> roles = roleRepository.findAll();
        if (roles.isEmpty()) {
            throw new NotFoundException("No roles found");
        }
        return roleMapper.toResponseList(roles);
    }

    @Override
    public RoleResponse create(CreateRoleRequest req) {
        if (roleRepository.existsByName(req.name)) {
            throw new BusinessException("Role name already exists");
        }

        Role role = new Role();
        roleMapper.fromCreateDto(req, role);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toResponse(savedRole);
    }

    @Override
    public RoleResponse update(Long id, UpdateRoleRequest req) {
        Role r = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role with id " + id + " not found"));
        roleMapper.fromUpdateDto(req, r);
        Role updatedRole = roleRepository.save(r);
        return roleMapper.toResponse(updatedRole);
    }

    @Override
    public void delete(Long id) {
        Role r = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role with id " + id + " not found"));

        if (userRepository.existsByRoles_Id(id)) {
            throw new BusinessException("Role is assigned to users. Cannot delete.");
        }

        roleRepository.delete(r);
    }

    @Override
    public RoleResponse updatePermissions(Long id, RolePermissionsRequest req) {
        Role r = roleRepository.findById(id).orElseThrow(() -> new NotFoundException("Role with id " + id + " not found"));
        roleMapper.fromPermissionDto(req, r);
        Role updatedRole = roleRepository.save(r);
        return roleMapper.toResponse(updatedRole);
    }

    @Override
    public RoleResponse getById(Long id) {
        Role r = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role with id " + id + " not found"));
        return roleMapper.toResponse(r);
    }

}
