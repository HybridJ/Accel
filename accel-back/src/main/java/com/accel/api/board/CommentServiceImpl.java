package com.accel.api.board;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl implements CommentService{

	private final CommentDao commentDao;

	public CommentServiceImpl(CommentDao commentDao) {
		this.commentDao = commentDao;
	}

	@Override
	public List<Comment> selectAllComments(int articleId, String userId) {
		return commentDao.selectAllComments(articleId, userId);
	}

	@Override
	public int insertComment(Comment comment) {
		return commentDao.insertComment(comment);
	}

	@Override
	public Comment selectComment(int commentId) {
		return commentDao.selectComment(commentId);
	}

	@Override
	public int updateComment(Comment comment) {
		return commentDao.updateComment(comment);
	}

	@Override
	@Transactional
	public int deleteComment(int commentId) {
		commentDao.deleteCommentLikesByCommentId(commentId);
		return commentDao.deleteComment(commentId);
	}

	@Override
	public CommentLikeStatus getCommentLikeStatus(int commentId, String userId) {
		int likeCount = commentDao.countCommentLikes(commentId);
		boolean liked = userId != null && !userId.isBlank()
				&& commentDao.existsCommentLike(commentId, userId) > 0;
		return new CommentLikeStatus(likeCount, liked);
	}

	@Override
	@Transactional
	public CommentLikeStatus likeComment(int commentId, String userId) {
		commentDao.insertCommentLike(commentId, userId);
		return getCommentLikeStatus(commentId, userId);
	}

	@Override
	@Transactional
	public CommentLikeStatus unlikeComment(int commentId, String userId) {
		commentDao.deleteCommentLike(commentId, userId);
		return getCommentLikeStatus(commentId, userId);
	}

}
