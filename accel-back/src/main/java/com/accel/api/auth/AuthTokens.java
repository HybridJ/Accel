package com.accel.api.auth;

public record AuthTokens(LoginResponse loginResponse, String refreshToken, long refreshMaxAgeSeconds) {
}
