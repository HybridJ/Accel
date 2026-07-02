package com.accel.api.auth;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.accel.api.user.User;

public interface AuthService {
	
	// Auth-01 회원가입
	// password를 제외한 User
	int signUp(User user, MultipartFile profileImage) throws IOException;
	
	// Auth-02 로그인 (authenticate)
	// password를 제외한 User
	AuthTokens login(LoginRequest request);

	AuthTokens refresh(String refreshToken);

	void logout(String refreshToken, String accessToken);
	
	// Auth-03 로그아웃
	// Service 필요x
	
}
