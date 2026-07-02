package com.accel.api.user;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.accel.api.board.Board;

@Mapper
public interface UserDao {

	@Select("""
		SELECT user_id, role, name, nickname, age, email, profile_img
		FROM user
		ORDER BY user_id
		""")
	List<User> selectAllUsers();

	User selectById(String userId);

	// 마이페이지 목록 조회 (User-04/05/06)
	List<Board> selectMyBoards(@Param("userId") String userId);          // 작성한 글

	List<Board> selectMyCommentedBoards(@Param("userId") String userId); // 댓글 단 글(게시글 distinct)

	List<Board> selectMyLikedBoards(@Param("userId") String userId);     // 좋아요한 글

	// 마이페이지 수정 (User-02) — 닉네임 중복 검사 및 정보 갱신
	int countByNickname(@Param("nickname") String nickname);

	int updateUser(User user);

	// 회원 탈퇴 (User-07) — 연관 데이터 선삭제 후 회원 삭제
	@Delete("""
		DELETE cl
		FROM comment_like cl
		LEFT JOIN comment c ON cl.comment_id = c.comment_id
		LEFT JOIN article a ON c.article_id = a.article_id
		WHERE cl.user_id = #{userId}
		   OR c.user_id = #{userId}
		   OR a.user_id = #{userId}
		""")
	int deleteCommentLikesOfUser(@Param("userId") String userId);

	int deleteArticleLikesOfUser(@Param("userId") String userId);

	int deleteCommentsOfUser(@Param("userId") String userId);

	int deleteArticlesOfUser(@Param("userId") String userId);

	int deleteUser(@Param("userId") String userId);
}
