package com.accel.api.board;

import java.util.List;

public interface BoardService {
	
	// 모든 게시판 조회
	List<Board> selectAllBoards();
	
	// 특정 게시"판" 가져오기
	List<Board> selectBoard(int brandId);

	// 특정 게시"판" 가져오기 (브랜드 슬러그로)
	List<Board> selectBoardBySlug(String brandSlug);
	
	// 특정 게시"글" 가져오기
	Board selectArticle(int articleId);

	// 게시글 조회수 증가
	Board increaseArticleViewCount(int articleId);
	
	// 게시글 등록
	// userId, title, content, testDriveAvailable, brandId, brandName
	int insertArticle(Board board);

	int insertArticleImage(int articleId, String imageUrl, int displayOrder);

	List<String> selectArticleImages(int articleId);

	int deleteArticleImages(int articleId);
	
	// 게시글 수정
	// articleId, userId, title, content, testDriveAvailable
	int updateArticle(Board board);
	
	// 게시글 삭제
	// 반환값x
	int deleteArticle(int articleId);

	// 게시글 좋아요 상태 조회 (userId가 없으면 liked는 false)
	ArticleLikeStatus getArticleLikeStatus(int articleId, String userId);

	// 게시글 좋아요 등록
	ArticleLikeStatus likeArticle(int articleId, String userId);

	// 게시글 좋아요 취소
	ArticleLikeStatus unlikeArticle(int articleId, String userId);

	// 사용자가 작성한 글 목록
	List<Board> selectArticlesByUser(String userId);

	// 사용자가 좋아요한 글 목록
	List<Board> selectLikedArticlesByUser(String userId);

	// 사용자가 댓글을 단 글 목록
	List<Board> selectCommentedArticlesByUser(String userId);
}

