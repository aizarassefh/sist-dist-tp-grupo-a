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
            return new ResponseEntity<>(
                    new ErrorResponse(
                            HttpStatus.UNAUTHORIZED,
                            "Missing or invalid Authorization header.",
                            null,
                            null
                    ),
                    HttpStatus.UNAUTHORIZED
            );
        }

        String token = authHeader.substring(7);

        var requiredRoles = Arrays.asList(requiresRoles.value());
        boolean allowed = JwtHelper.verifyUserRoleFromToken(
                token,
                requiredRoles,
                secret
        );

        if (!allowed) {
            return new ResponseEntity<>(
                    new ErrorResponse(
                            HttpStatus.FORBIDDEN,
                            "User does not have permissions to perform this action.",
                            null,
                            null
                    ),
                    HttpStatus.FORBIDDEN
            );
        }

        return joinPoint.proceed();
    }
}