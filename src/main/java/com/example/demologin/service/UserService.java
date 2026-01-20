package com.example.demologin.service;

import com.example.demologin.dto.request.user.UpdateUserRequest;
import com.example.demologin.dto.response.MemberResponse;
import org.springframework.data.domain.Page;

public interface UserService {
    Page<MemberResponse> getAllUsers(int page, int size);
} 
