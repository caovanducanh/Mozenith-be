package com.example.demologin.service;

import com.example.demologin.entity.User;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.serviceImpl.UserServiceImpl;
import com.example.demologin.mapper.UserMapper;
import com.example.demologin.dto.response.MemberResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// ...existing code...
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	@InjectMocks
	private UserServiceImpl userService;

	@Mock
	private UserRepository userRepository;

	// ...existing code...

	@Mock
	private UserMapper userMapper;

	@Test
	void testGetAllUsers() {
		User user = new User();
		org.springframework.data.domain.Page<User> userPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));
		MemberResponse memberResponse = new MemberResponse();
		when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(userPage);
		when(userMapper.toUserResponse(user)).thenReturn(memberResponse);
		var result = userService.getAllUsers(0, 10);
		assertEquals(1, result.getTotalElements());
		assertEquals(memberResponse, result.getContent().get(0));
	}
}
