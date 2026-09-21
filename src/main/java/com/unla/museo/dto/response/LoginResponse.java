package com.unla.museo.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer", 900);
    }
}