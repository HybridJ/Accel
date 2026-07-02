package com.accel.api.auth;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RefreshToken {
	private String tokenHash;
	private String userId;
	private LocalDateTime expiresAt;
	private boolean revoked;
	private LocalDateTime createdAt;
}
