package com.unla.museo.aspect;

import com.unla.museo.controllers.annotations.RequiresRoles;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAspectTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private HttpServletRequest request;

    private RoleAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RoleAspect();
        ReflectionTestUtils.setField(aspect, "secret", SECRET);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn(null);
        RequiresRoles annotation = annotation("CURADOR");

        Object result = aspect.checkRoles(joinPoint, annotation);

        assertEquals(401, ((org.springframework.http.ResponseEntity<?>) result).getStatusCode().value());
        verifyNoInteractions(joinPoint);
    }

    @Test
    void proceedsWhenTokenRoleMatchesAnyRequiredRole() throws Throwable {
        String token = JwtTestTokenFactory.create("CURADOR", SECRET);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        RequiresRoles annotation = annotation("ADMINISTRADOR", "CURADOR");
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.checkRoles(joinPoint, annotation));
        verify(joinPoint).proceed();
    }

    @Test
    void returnsForbiddenWhenRoleDoesNotMatch() throws Throwable {
        String token = JwtTestTokenFactory.create("VISITANTE", SECRET);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        RequiresRoles annotation = annotation("ADMINISTRADOR", "CURADOR");

        Object result = aspect.checkRoles(joinPoint, annotation);

        assertEquals(403, ((org.springframework.http.ResponseEntity<?>) result).getStatusCode().value());
        verifyNoInteractions(joinPoint);
    }

    @Test
    void returnsUnauthorizedForInvalidToken() throws Throwable {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        RequiresRoles annotation = annotation("CURADOR");

        Object result = aspect.checkRoles(joinPoint, annotation);

        assertEquals(401, ((org.springframework.http.ResponseEntity<?>) result).getStatusCode().value());
        verifyNoInteractions(joinPoint);
    }

    private RequiresRoles annotation(String... roles) {
        try {
            Method method = RoleAspectTest.class.getDeclaredMethod("annotatedMethod");
            RequiresRoles annotation = method.getAnnotation(RequiresRoles.class);
            return new RequiresRoles() {
                @Override public String[] value() { return roles; }
                @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RequiresRoles.class; }
            };
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @RequiresRoles({"CURADOR"})
    private void annotatedMethod() {
    }
}
