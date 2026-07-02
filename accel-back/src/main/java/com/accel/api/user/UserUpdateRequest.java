package com.accel.api.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 수정 요청 DTO (User-02)
 * - 아이디(userId), 비밀번호(password), 권한(role)은 변경 불가하므로 의도적으로 제외한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
	private String name;
	private String nickname;
	private Integer age;
	private String email;
}
