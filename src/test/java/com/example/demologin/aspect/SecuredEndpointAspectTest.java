
package com.example.demologin.aspect;

import java.lang.reflect.Method;
import com.example.demologin.annotation.SecuredEndpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecuredEndpointAspectTest {
	@Test
	void testResolveAnnotation_realClassAnnotation() throws Exception {
		// Tạo instance thực sự của class có annotation
		@SecuredEndpoint("REAL_CLASS_ANN")
		class RealAnnotatedClass {
			public void realMethod() {}
		}
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		Method m = RealAnnotatedClass.class.getDeclaredMethod("realMethod");
		when(methodSignature.getMethod()).thenReturn(m);
		java.lang.reflect.Method resolve = SecuredEndpointAspect.class.getDeclaredMethod("resolveAnnotation", org.aspectj.lang.ProceedingJoinPoint.class, com.example.demologin.annotation.SecuredEndpoint.class);
		resolve.setAccessible(true);
		Object result = resolve.invoke(aspect, joinPoint, null);
		assertNotNull(result);
		assertEquals("REAL_CLASS_ANN", ((com.example.demologin.annotation.SecuredEndpoint) result).value());
	}
	@SecuredEndpoint("PARENT_ANN")
	static class ParentClass {
		public void parentMethod() {}
	}
	static class ChildClass extends ParentClass {
		@Override public void parentMethod() {}
	}

	@Test
	void testResolveAnnotation_parentClassAnnotation() throws Exception {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		// Lấy method override ở class con, không có annotation, class cha có annotation
		Method m = ChildClass.class.getMethod("parentMethod");
		when(methodSignature.getMethod()).thenReturn(m);
		java.lang.reflect.Method resolve = SecuredEndpointAspect.class.getDeclaredMethod("resolveAnnotation", org.aspectj.lang.ProceedingJoinPoint.class, com.example.demologin.annotation.SecuredEndpoint.class);
		resolve.setAccessible(true);
		Object result = resolve.invoke(aspect, joinPoint, null);
		// Nếu annotation không được kế thừa, sẽ trả về null, nếu kế thừa sẽ trả về annotation của class cha
		// Đảm bảo nhánh getDeclaringClass().getAnnotation được chạy
		// Jacoco sẽ nhận diện nhánh này
		assertNull(result); // Java không tự động kế thừa annotation cho method override
	}
	static class NoAnnotationClass {
		public void dummyMethod() {}
	}

	@Test
	void testResolveAnnotation_methodAndClassNoAnnotation() throws Exception {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		Method m = NoAnnotationClass.class.getMethod("dummyMethod");
		when(methodSignature.getMethod()).thenReturn(m);
		java.lang.reflect.Method resolve = SecuredEndpointAspect.class.getDeclaredMethod("resolveAnnotation", org.aspectj.lang.ProceedingJoinPoint.class, com.example.demologin.annotation.SecuredEndpoint.class);
		resolve.setAccessible(true);
		Object result = resolve.invoke(aspect, joinPoint, null);
		assertNull(result);
	}
	@SecuredEndpoint("CLASS_ANN")
	static class AnnotatedClass {
		public void methodWithoutAnnotation() {}
	}

	@Test
	void testResolveAnnotation_classAnnotation() throws Exception {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		// Lấy method thuộc class AnnotatedClass, method không có annotation nhưng class có annotation
		Method m = AnnotatedClass.class.getDeclaredMethod("methodWithoutAnnotation");
		when(methodSignature.getMethod()).thenReturn(m);
		java.lang.reflect.Method resolve = SecuredEndpointAspect.class.getDeclaredMethod("resolveAnnotation", org.aspectj.lang.ProceedingJoinPoint.class, com.example.demologin.annotation.SecuredEndpoint.class);
		resolve.setAccessible(true);
		Object result = resolve.invoke(aspect, joinPoint, null);
		assertNotNull(result);
		assertEquals("CLASS_ANN", ((com.example.demologin.annotation.SecuredEndpoint) result).value());
	}
	@Test
	void testCheckSecuredEndpoint_tokenValidationException() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
		lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
		lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
		when(context.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(context);
		when(request.getHeader("Authorization")).thenReturn("Bearer token");
		when(jwtUtil.extractRoles(anyString())).thenThrow(new com.example.demologin.exception.exceptions.TokenValidationException("msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.EXPIRED));
		var ex = assertThrows(com.example.demologin.exception.exceptions.InvalidTokenException.class, () -> aspect.checkSecuredEndpoint(joinPoint, annotation));
		assertEquals("Token has expired", ex.getMessage());
	}
	@Test
	void testCheckSecuredEndpoint_authenticationNotAuthenticated() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
		lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
		lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(false);
		org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
		when(context.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(context);
		assertThrows(com.example.demologin.exception.exceptions.UserNotAuthenticatedException.class,
			() -> aspect.checkSecuredEndpoint(joinPoint, annotation));
	}
	@Test
	void testResolveAnnotation_methodAndClass() throws Exception {
		// Prepare mocks
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		Method m = DummyClass.class.getMethod("dummyMethod");
		when(methodSignature.getMethod()).thenReturn(m);
		// No annotation on method or class
		java.lang.reflect.Method resolve = SecuredEndpointAspect.class.getDeclaredMethod("resolveAnnotation", org.aspectj.lang.ProceedingJoinPoint.class, com.example.demologin.annotation.SecuredEndpoint.class);
		resolve.setAccessible(true);
		Object result = resolve.invoke(aspect, joinPoint, null);
		assertNull(result);
	}

	static class DummyClass {
		public void dummyMethod() {}
	}

	@Test
	void testExtractTokenFromRequest_invalidHeader() throws Exception {
		java.lang.reflect.Method m = SecuredEndpointAspect.class.getDeclaredMethod("extractTokenFromRequest");
		m.setAccessible(true);
		when(request.getHeader("Authorization")).thenReturn("invalid");
		Exception ex = assertThrows(Exception.class, () -> m.invoke(aspect));
		Throwable cause = ex.getCause();
		assertTrue(cause instanceof com.example.demologin.exception.exceptions.InvalidTokenException);
		assertTrue(cause.getMessage().contains("Invalid authorization token format"));
	}

	@Test
	void testExtractTokenFromRequest_validHeader() throws Exception {
		java.lang.reflect.Method m = SecuredEndpointAspect.class.getDeclaredMethod("extractTokenFromRequest");
		m.setAccessible(true);
		when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");
		String token = (String) m.invoke(aspect);
		assertEquals("abc.def.ghi", token);
	}

	private org.aspectj.lang.Signature mockSignature(String name) {
		return new org.aspectj.lang.Signature() {
			@Override public String toShortString() { return name; }
			@Override public String toLongString() { return name; }
			@Override public String getName() { return name; }
			@Override public int getModifiers() { return 0; }
			@Override public Class<?> getDeclaringType() { return Object.class; }
			@Override public String getDeclaringTypeName() { return "Object"; }
		};
	}

	@Test
	void testHandleTokenValidationException_expired() {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(mockSignature("testMethod"));
		var ex = new com.example.demologin.exception.exceptions.TokenValidationException(
				"msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.EXPIRED);
		String msg = invokeHandleTokenValidationException(aspect, ex, joinPoint);
		assertEquals("Token has expired", msg);
	}

	@Test
	void testHandleTokenValidationException_invalidSignature() {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(mockSignature("testMethod"));
		var ex = new com.example.demologin.exception.exceptions.TokenValidationException(
				"msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.INVALID_SIGNATURE);
		String msg = invokeHandleTokenValidationException(aspect, ex, joinPoint);
		assertEquals("Invalid token signature", msg);
	}

	@Test
	void testHandleTokenValidationException_missingPermissions() {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(mockSignature("testMethod"));
		var ex = new com.example.demologin.exception.exceptions.TokenValidationException(
				"msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.MISSING_PERMISSIONS);
		String msg = invokeHandleTokenValidationException(aspect, ex, joinPoint);
		assertEquals("Invalid token permissions format", msg);
	}

	@Test
	void testHandleTokenValidationException_malformed() {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(mockSignature("testMethod"));
		var ex = new com.example.demologin.exception.exceptions.TokenValidationException(
				"msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.MALFORMED);
		String msg = invokeHandleTokenValidationException(aspect, ex, joinPoint);
		assertEquals("Malformed token", msg);
	}

	@Test
	void testHandleTokenValidationException_default() {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(mockSignature("testMethod"));
		var ex = new com.example.demologin.exception.exceptions.TokenValidationException(
				"msg", com.example.demologin.exception.exceptions.TokenValidationException.TokenErrorType.GENERAL_ERROR);
		String msg = invokeHandleTokenValidationException(aspect, ex, joinPoint);
		assertEquals("Token validation failed", msg);
	}

	// Helper to access private method
	private String invokeHandleTokenValidationException(SecuredEndpointAspect aspect, com.example.demologin.exception.exceptions.TokenValidationException ex, org.aspectj.lang.ProceedingJoinPoint joinPoint) {
		try {
			java.lang.reflect.Method m = SecuredEndpointAspect.class.getDeclaredMethod("handleTokenValidationException", com.example.demologin.exception.exceptions.TokenValidationException.class, org.aspectj.lang.ProceedingJoinPoint.class);
			m.setAccessible(true);
			return (String) m.invoke(aspect, ex, joinPoint);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	@InjectMocks
	private SecuredEndpointAspect aspect;

	@org.mockito.Mock
	private jakarta.servlet.http.HttpServletRequest request;
	@org.mockito.Mock
	private com.example.demologin.utils.JwtUtil jwtUtil;
	@org.mockito.Mock
	private com.example.demologin.service.RolePermissionService rolePermissionService;

	@Test
	void testAspectNotNull() {
		assertNotNull(aspect);
	}

	@Test
	void testCheckSecuredEndpoint_noAnnotation() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
	lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
	lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
		assertThrows(com.example.demologin.exception.exceptions.AccessDenyException.class,
			() -> aspect.checkSecuredEndpoint(joinPoint, null));
	}

	@Test
	void testCheckSecuredEndpoint_unauthenticated() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
	lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
	lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
	lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
		assertThrows(com.example.demologin.exception.exceptions.UserNotAuthenticatedException.class,
			() -> aspect.checkSecuredEndpoint(joinPoint, annotation));
	}

	@Test
	void testCheckSecuredEndpoint_missingToken() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
	lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
	lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
	lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
		when(context.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(context);
		when(request.getHeader("Authorization")).thenReturn(null);
		assertThrows(com.example.demologin.exception.exceptions.InvalidTokenException.class,
			() -> aspect.checkSecuredEndpoint(joinPoint, annotation));
	}

	@Test
	void testCheckSecuredEndpoint_permissionDenied() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
	lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
	lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
	lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
		when(context.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(context);
		when(request.getHeader("Authorization")).thenReturn("Bearer token");
		when(jwtUtil.extractRoles(anyString())).thenReturn(java.util.Set.of("ROLE_USER"));
		when(jwtUtil.extractUsernameWithValidation(anyString())).thenReturn("user");
		when(rolePermissionService.getPermissionsForRoles(anySet())).thenReturn(java.util.Set.of("OTHER"));
		assertThrows(com.example.demologin.exception.exceptions.AccessDenyException.class,
			() -> aspect.checkSecuredEndpoint(joinPoint, annotation));
	}

	@Test
	void testCheckSecuredEndpoint_permissionGranted() throws Throwable {
		var joinPoint = mock(org.aspectj.lang.ProceedingJoinPoint.class);
		var annotation = mock(com.example.demologin.annotation.SecuredEndpoint.class);
		var methodSignature = mock(org.aspectj.lang.reflect.MethodSignature.class);
	lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
	lenient().when(methodSignature.getMethod()).thenReturn(Object.class.getMethod("toString"));
	lenient().when(annotation.value()).thenReturn("PERM");
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
		when(context.getAuthentication()).thenReturn(auth);
		org.springframework.security.core.context.SecurityContextHolder.setContext(context);
		when(request.getHeader("Authorization")).thenReturn("Bearer token");
		when(jwtUtil.extractRoles(anyString())).thenReturn(java.util.Set.of("ROLE_USER"));
		when(jwtUtil.extractUsernameWithValidation(anyString())).thenReturn("user");
		when(rolePermissionService.getPermissionsForRoles(anySet())).thenReturn(java.util.Set.of("PERM"));
		when(joinPoint.proceed()).thenReturn("ok");
		Object result = aspect.checkSecuredEndpoint(joinPoint, annotation);
		assertEquals("ok", result);
	}
}
