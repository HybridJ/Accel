package com.accel.api.user;

import java.util.List;

import com.accel.api.board.Board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 다른 유저 공개 프로필 응답 (No.11)
 * - grade, myCar 는 현재 스키마에 없어 제외. 등급/마이카 도입 시 필드만 추가하면 된다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
	private String nickname;
	private String profileImg;
	private List<Board> posts; // 해당 유저가 작성한 글 목록
}
