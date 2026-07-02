package com.accel.api.board;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@RequestMapping("/comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	/**
	 * Comment-01 한 게시글의 모든 댓글 조회
	 */
	@Operation(summary = "댓글 목록 조회", description = "게시글 ID에 해당하는 모든 댓글을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (댓글 없으면 빈 배열)")
	@GetMapping("/{articleId}")
	public ResponseEntity<List<Comment>> commentList(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			Authentication authentication) {
		List<Comment> comments = commentService.selectAllComments(articleId, currentUserId(authentication));
		return ResponseEntity.ok(comments != null ? comments : List.of());
	}

	/**
	 * Comment-02 게시글에 댓글 작성
	 */
	@Operation(summary = "댓글 작성", description = "게시글 ID에 해당하는 게시글에 댓글을 작성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락 등)")
	})
	@PostMapping("/{articleId}")
	public ResponseEntity<Comment> createComment(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			@RequestBody Comment comment,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		comment.setArticleId(articleId);
		comment.setUserId(userId);
		try {
			commentService.insertComment(comment);
			return ResponseEntity.status(HttpStatus.CREATED).body(comment);
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Comment-03 게시글 댓글 수정
	 */
	@Operation(summary = "댓글 수정", description = "댓글 ID에 해당하는 댓글을 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
	})
	@PutMapping("/{articleId}/{commentId}")
	public ResponseEntity<Comment> updateComment(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			@Parameter(description = "댓글 ID", example = "1") @PathVariable int commentId,
			@RequestBody Comment comment,
			Authentication authentication) {
		Comment searchComment = commentService.selectComment(commentId);
		if (searchComment == null || !Integer.valueOf(articleId).equals(searchComment.getArticleId())) {
			return ResponseEntity.notFound().build();
		}

		if (!canManageComment(searchComment, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		comment.setCommentId(commentId);
		comment.setArticleId(articleId);
		comment.setUserId(searchComment.getUserId());
		commentService.updateComment(comment);
		return ResponseEntity.ok(comment);
	}

	/**
	 * Comment-04 게시글 댓글 삭제
	 */
	@Operation(summary = "댓글 삭제", description = "댓글 ID에 해당하는 댓글을 삭제합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
	})
	@DeleteMapping("/{articleId}/{commentId}")
	public ResponseEntity<Void> deleteComment(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			@Parameter(description = "댓글 ID", example = "1") @PathVariable int commentId,
			Authentication authentication) {
		Comment searchComment = commentService.selectComment(commentId);
		if (searchComment == null || !Integer.valueOf(articleId).equals(searchComment.getArticleId())) {
			return ResponseEntity.notFound().build();
		}

		if (!canManageComment(searchComment, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		commentService.deleteComment(commentId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Comment-05 댓글 좋아요 등록 POST /comments/{commentId}/likes
	 */
	@Operation(summary = "댓글 좋아요 등록", description = "댓글에 좋아요를 등록합니다.")
	@ApiResponse(responseCode = "200", description = "등록 성공")
	@PostMapping("/{commentId}/likes")
	public ResponseEntity<CommentLikeStatus> likeComment(
			@Parameter(description = "댓글 ID", example = "1") @PathVariable int commentId,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(commentService.likeComment(commentId, userId));
	}

	/**
	 * Comment-06 댓글 좋아요 취소 DELETE /comments/{commentId}/likes
	 */
	@Operation(summary = "댓글 좋아요 취소", description = "댓글의 좋아요를 취소합니다.")
	@ApiResponse(responseCode = "200", description = "취소 성공")
	@DeleteMapping("/{commentId}/likes")
	public ResponseEntity<CommentLikeStatus> unlikeComment(
			@Parameter(description = "댓글 ID", example = "1") @PathVariable int commentId,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(commentService.unlikeComment(commentId, userId));
	}

	private String currentUserId(Authentication authentication) {
		if (authentication == null
				|| authentication instanceof AnonymousAuthenticationToken
				|| !authentication.isAuthenticated()) {
			return null;
		}

		return authentication.getName();
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication != null
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	private boolean canManageComment(Comment comment, Authentication authentication) {
		String userId = currentUserId(authentication);
		return userId != null && (userId.equals(comment.getUserId()) || isAdmin(authentication));
	}

	private HttpStatus ownerErrorStatus(Authentication authentication) {
		return currentUserId(authentication) == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
	}
}
