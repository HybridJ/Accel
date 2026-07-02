package com.accel.api.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
	private String accessToken;
	private String tokenType; // "Bearer"
    private String username;
    private String role;
}
