package com.accel.api.user;

/**
 * 마이페이지 수정 시 닉네임이 이미 사용 중일 때 발생 (User-02, 409 Conflict).
 */
public class DuplicateNicknameException extends RuntimeException {
	public DuplicateNicknameException(String nickname) {
		super("이미 사용 중인 닉네임입니다: " + nickname);
	}
}
