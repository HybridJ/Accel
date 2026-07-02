package com.accel.api.board;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentDao {
	
	List<Comment> selectAllComments(@Param("articleId") int articleId, @Param("userId") String userId);

	int insertComment(@Param("comment") Comment comment);

	Comment selectComment(@Param("commentId") int commentId);

	int updateComment(@Param("comment") Comment comment);

	int deleteComment(@Param("commentId") int commentId);

	int insertCommentLike(@Param("commentId") int commentId, @Param("userId") String userId);

	int deleteCommentLike(@Param("commentId") int commentId, @Param("userId") String userId);

	int deleteCommentLikesByCommentId(@Param("commentId") int commentId);

	int countCommentLikes(@Param("commentId") int commentId);

	int existsCommentLike(@Param("commentId") int commentId, @Param("userId") String userId);

}
