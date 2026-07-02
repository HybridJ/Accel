package com.accel.api.board;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardDao {
	
	List<Board> selectAllBoards();
	
	List<Board> selectBoard(int brandId);

	List<Board> selectBoardBySlug(String brandSlug);

	Board selectArticle(int articleId);

	int increaseArticleViewCount(@Param("articleId") int articleId);

	List<String> selectArticleImages(@Param("articleId") int articleId);

	int insertArticle(@Param("board") Board board);

	int insertArticleImage(
			@Param("articleId") int articleId,
			@Param("imageUrl") String imageUrl,
			@Param("displayOrder") int displayOrder);

	int deleteArticleImages(@Param("articleId") int articleId);

	int deleteArticleLikes(@Param("articleId") int articleId);

	int deleteCommentLikesByArticleId(@Param("articleId") int articleId);

	int deleteCommentsByArticleId(@Param("articleId") int articleId);

	int updateArticle(@Param("board") Board board);

	int deleteArticle(@Param("articleId") int articleId);

	int insertArticleLike(@Param("articleId") int articleId, @Param("userId") String userId);

	int deleteArticleLike(@Param("articleId") int articleId, @Param("userId") String userId);

	int countArticleLikes(@Param("articleId") int articleId);

	int existsArticleLike(@Param("articleId") int articleId, @Param("userId") String userId);

	List<Board> selectArticlesByUser(@Param("userId") String userId);

	List<Board> selectLikedArticlesByUser(@Param("userId") String userId);

	List<Board> selectCommentedArticlesByUser(@Param("userId") String userId);
}
