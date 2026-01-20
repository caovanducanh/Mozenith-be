package com.example.demologin.serviceImpl;

import com.example.demologin.annotation.SmartCache;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.RoleRepository;

import com.example.demologin.service.RolePermissionService;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class RolePermissionServiceImpl implements RolePermissionService {
    private final RoleRepository roleRepository;

    @Override
    @SmartCache
    public Set<String> getPermissionsForRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptySet();
        }

        // Get all roles by name
        Set<Role> roles = new HashSet<>(roleRepository.findAllByNameIn(roleNames));

        // Extract all permission codes
        return roles.stream()
                .filter(Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());



    }
}
