package com.unla.museo.aspect;

import com.unla.museo.controllers.annotations.RequiresRoles;
import com.unla.museo.dto.response.ErrorResponse;
import com.unla.museo.helper.JwtHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class RoleAspect {

    @Value("${jwt.secret}")
    private String secret;

    @Around("@annotation(requiresRoles)")
    public Object checkRoles(ProceedingJoinPoint joinPoint, RequiresRoles requiresRoles) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse("Missing or invalid Authorization header.");
        }

        String token = authHeader.substring(7);
        List<String> requiredRoles = Arrays.asList(requiresRoles.value());

        try {
            boolean allowed = JwtHelper.verifyUserRoleFromToken(token, requiredRoles, secret);
            if (!allowed) {
                return forbiddenResponse();
            }
        } catch (IllegalArgumentException exception) {
            return unauthorizedResponse("Invalid or expired token.");
        }

        return joinPoint.proceed();
    }

    private ResponseEntity<ErrorResponse> unauthorizedResponse(String message) {
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.UNAUTHORIZED, message, null, null),
                HttpStatus.UNAUTHORIZED
        );
    }

    private ResponseEntity<ErrorResponse> forbiddenResponse() {
        return new ResponseEntity<>(
                new ErrorResponse(
                        HttpStatus.FORBIDDEN,
                        "User role is not authorized to perform this action.",
                        null,
                        null
                ),
                HttpStatus.FORBIDDEN
        );
    }
}
