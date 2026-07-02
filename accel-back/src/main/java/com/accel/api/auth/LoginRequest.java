package com.accel.api.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
// Login 요청을 받을 때 로그인 정보를 담는 객체
public class LoginRequest {
	@NotBlank
	private String userId; // accel 프로젝트에서는 username이 아니라 userId
	@NotBlank
	private String password;
}
