package com.accel.api.board;

import java.util.List;

public interface CommentService {
	// 한 게시글의 모든 댓글 반환 (userId가 있으면 해당 사용자의 좋아요 여부 포함)
	List<Comment> selectAllComments(int articleId, String userId);

	// 게시글에 댓글 작성
	// comment_id, user_id, content
	int insertComment(Comment comment);

	// commentId 기준으로 댓글 검색
	// comment_id, user_id, content, article_id
	Comment selectComment(int commentId);

	// 댓글 수정
	int updateComment(Comment comment);

	// 댓글 삭제
	int deleteComment(int commentId);

	// 댓글 좋아요 상태 조회 (userId가 없으면 liked는 false)
	CommentLikeStatus getCommentLikeStatus(int commentId, String userId);

	// 댓글 좋아요 등록
	CommentLikeStatus likeComment(int commentId, String userId);

	// 댓글 좋아요 취소
	CommentLikeStatus unlikeComment(int commentId, String userId);
}
