package com.example.demologin.controller;

import com.example.demologin.dto.response.MemberResponse;
import com.example.demologin.dto.response.UserResponse;
import com.example.demologin.entity.User;
import com.example.demologin.service.UserService;
import com.example.demologin.utils.AccountUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
	@InjectMocks
	private UserController userController;

	@Mock
	private UserService userService;

	@Mock
	private AccountUtils accountUtils;

	@Test
	void testGetAllUsers_emptyPage() {
		Page<MemberResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
		when(userService.getAllUsers(0, 20)).thenReturn(emptyPage);
		Object result = userController.getAllUsers(0, 20);
		assertNotNull(result);
		assertTrue(result instanceof Page);
		assertTrue(((Page<?>) result).isEmpty());
	}

	@Test
	void testGetCurrentUserProfile() {
	User user = new User();
	when(accountUtils.getCurrentUser()).thenReturn(user);
	Object result = userController.getCurrentUserProfile();
	assertNotNull(result);
	assertTrue(result instanceof UserResponse);
	}
}

