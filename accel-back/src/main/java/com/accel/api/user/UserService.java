package com.accel.api.user;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.accel.api.board.Board;

public interface UserService {
	// 모든 유저 조회
	List<User> getAllUsers();

	// 아이디로 유저 조회
	User getById(String userId);

	// 마이페이지 조회 (User-01)
	User getMyPage(String userId);

	// 마이페이지 수정 (User-02)
	User updateMyPage(String userId, UserUpdateRequest request);

	User updateMyPage(String userId, UserUpdateRequest request, MultipartFile profileImage) throws IOException;

	User updateProfileImage(String userId, MultipartFile profileImage) throws IOException;

	// 회원 탈퇴 (User-07)
	void deleteMyPage(String userId);

	// 마이페이지 목록 조회 (User-04/05/06)
	List<Board> getMyBoards(String userId);          // 작성한 글

	List<Board> getMyCommentedBoards(String userId); // 댓글 단 글

	List<Board> getMyLikedBoards(String userId);     // 좋아요한 글

	// 다른 유저 공개 프로필 조회 (No.11)
	UserProfileResponse getUserProfile(String userId);
}
