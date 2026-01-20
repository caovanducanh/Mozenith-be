package com.example.demologin.serviceImpl;

import com.example.demologin.entity.User;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.UserActivityLogRepository;
import com.example.demologin.dto.request.user.UserRegistrationRequest;
import com.example.demologin.enums.Gender;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.exception.exceptions.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
	@InjectMocks
	private AuthenticationServiceImpl authenticationService;

	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private UserActivityLogRepository userActivityLogRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void testRegister_passwordNotMatch() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("456");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		assertThrows(ValidationException.class, () -> authenticationService.register(req));
	}

	@Test
	void testRegister_usernameExists() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("123");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		when(userRepository.existsByUsername("user")).thenReturn(true);
		assertThrows(ConflictException.class, () -> authenticationService.register(req));
	}

	@Test
	void testRegister_emailExists() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("123");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		when(userRepository.existsByUsername("user")).thenReturn(false);
		when(userRepository.existsByEmail("a@a.com")).thenReturn(true);
		assertThrows(ConflictException.class, () -> authenticationService.register(req));
	}

	@Test
	void testRegister_roleNotFound() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("123");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		when(userRepository.existsByUsername("user")).thenReturn(false);
		when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
		when(roleRepository.findByName("MEMBER")).thenReturn(java.util.Optional.empty());
	assertThrows(InternalServerErrorException.class, () -> authenticationService.register(req));
	}

	@Test
	void testRegister_success() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("123");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		when(userRepository.existsByUsername("user")).thenReturn(false);
		when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
		Role role = new Role();
		role.setName("MEMBER");
		when(roleRepository.findByName("MEMBER")).thenReturn(java.util.Optional.of(role));
		when(passwordEncoder.encode("123")).thenReturn("encoded");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		assertDoesNotThrow(() -> authenticationService.register(req));
	}

	@Test
	void testRegister_internalError() {
		UserRegistrationRequest req = new UserRegistrationRequest();
		req.setPassword("123");
		req.setConfirmPassword("123");
		req.setUsername("user");
		req.setEmail("a@a.com");
		req.setFullName("A");
		req.setDateOfBirth(java.time.LocalDate.of(2000,1,1));
		req.setGender(Gender.OTHER);
		req.setIdentityCard("123456789");
		req.setPhone("0123456789");
		req.setAddress("abc abc abc");
		when(userRepository.existsByUsername("user")).thenReturn(false);
		when(userRepository.existsByEmail("a@a.com")).thenReturn(false);
		Role role = new Role();
		role.setName("MEMBER");
		when(roleRepository.findByName("MEMBER")).thenReturn(java.util.Optional.of(role));
		when(passwordEncoder.encode("123")).thenReturn("encoded");
		when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("fail"));
		assertThrows(InternalServerErrorException.class, () -> authenticationService.register(req));
	}
}
