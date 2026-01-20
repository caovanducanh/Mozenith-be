package com.example.demologin.service;

import com.example.demologin.entity.User;
import com.example.demologin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Set;

public interface RolePermissionService {
    public Set<String> getPermissionsForRoles(Set<String> roleNames);
} 
