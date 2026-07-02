package com.accel.api.board;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardServiceImpl implements BoardService {
	
	private final BoardDao boardDao;
	
	public BoardServiceImpl(BoardDao boardDao) {
		this.boardDao = boardDao;
	}

	@Override
	public List<Board> selectAllBoards() {
		return boardDao.selectAllBoards();
	}

	@Override
	public List<Board> selectBoard(int brandId) {
		List<Board> boards = boardDao.selectBoard(brandId);
		boards.forEach(this::attachImages);
		return boards;
	}

	@Override
	public List<Board> selectBoardBySlug(String brandSlug) {
		List<Board> boards = boardDao.selectBoardBySlug(brandSlug);
		boards.forEach(this::attachImages);
		return boards;
	}

	@Override
	public Board selectArticle(int articleId) {
		Board board = boardDao.selectArticle(articleId);
		attachImages(board);
		return board; 
	}

	@Override
	@Transactional
	public Board increaseArticleViewCount(int articleId) {
		boardDao.increaseArticleViewCount(articleId);
		return selectArticle(articleId);
	}

	@Override
	public int insertArticle(Board board) {
		return boardDao.insertArticle(board);
	}

	@Override
	public int insertArticleImage(int articleId, String imageUrl, int displayOrder) {
		return boardDao.insertArticleImage(articleId, imageUrl, displayOrder);
	}

	@Override
	public List<String> selectArticleImages(int articleId) {
		return boardDao.selectArticleImages(articleId);
	}

	@Override
	public int deleteArticleImages(int articleId) {
		return boardDao.deleteArticleImages(articleId);
	}

	@Override
	public int updateArticle(Board board) {
		return boardDao.updateArticle(board);
	}

	@Override
	@Transactional
	public int deleteArticle(int articleId) {
		boardDao.deleteCommentLikesByArticleId(articleId);
		boardDao.deleteCommentsByArticleId(articleId);
		boardDao.deleteArticleLikes(articleId);
		boardDao.deleteArticleImages(articleId);
		return boardDao.deleteArticle(articleId);
	}

	@Override
	public ArticleLikeStatus getArticleLikeStatus(int articleId, String userId) {
		int likeCount = boardDao.countArticleLikes(articleId);
		boolean liked = userId != null && !userId.isBlank()
				&& boardDao.existsArticleLike(articleId, userId) > 0;
		return new ArticleLikeStatus(likeCount, liked);
	}

	@Override
	@Transactional
	public ArticleLikeStatus likeArticle(int articleId, String userId) {
		boardDao.insertArticleLike(articleId, userId);
		return getArticleLikeStatus(articleId, userId);
	}

	@Override
	@Transactional
	public ArticleLikeStatus unlikeArticle(int articleId, String userId) {
		boardDao.deleteArticleLike(articleId, userId);
		return getArticleLikeStatus(articleId, userId);
	}

	@Override
	public List<Board> selectArticlesByUser(String userId) {
		return boardDao.selectArticlesByUser(userId);
	}

	@Override
	public List<Board> selectLikedArticlesByUser(String userId) {
		return boardDao.selectLikedArticlesByUser(userId);
	}

	@Override
	public List<Board> selectCommentedArticlesByUser(String userId) {
		return boardDao.selectCommentedArticlesByUser(userId);
	}

	private void attachImages(Board board) {
		if (board == null || board.getArticleId() == null) {
			return;
		}

		List<String> imageUrls = boardDao.selectArticleImages(board.getArticleId());
		board.setImageUrls(imageUrls);

		if ((board.getImageUrl() == null || board.getImageUrl().isBlank()) && !imageUrls.isEmpty()) {
			board.setImageUrl(imageUrls.get(0));
		}
	}
	
}
