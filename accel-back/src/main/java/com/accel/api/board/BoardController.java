package com.accel.api.board;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.accel.api.storage.GcsStorageService;
import com.accel.api.storage.GcsStorageService.StoredImage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Board", description = "게시판 관련 API")
@RestController
@RequestMapping("/boards")
public class BoardController {

	private static final Logger log = LoggerFactory.getLogger(BoardController.class);
	private static final int MAX_ARTICLE_IMAGE_COUNT = 10;
	private static final String IMAGE_ORDER_EXISTING_PREFIX = "existing|";
	private static final String IMAGE_ORDER_NEW_PREFIX = "new|";

	private final BoardService boardService;
	private final GcsStorageService gcsStorageService;

	public BoardController(BoardService boardService, GcsStorageService gcsStorageService) {
		this.boardService = boardService;
		this.gcsStorageService = gcsStorageService;
	}

	/**
	 * Commu-1 게시판 목록 조회 (브랜드 목록 조회) GET /boards/brands
	 */
	@Operation(summary = "브랜드(게시판) 목록 조회", description = "모든 브랜드 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("/brands")
	public ResponseEntity<List<Board>> brandList() {
		List<Board> boards = boardService.selectAllBoards();
		return ResponseEntity.ok(boards != null ? boards : List.of());
	}

	/**
	 * Commu-2 특정 게시판 조회 GET /boards/brands/{brandId}
	 */
	@Operation(summary = "특정 브랜드 게시글 목록 조회", description = "브랜드 ID에 해당하는 게시글 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (게시글 없으면 빈 배열)")
	@GetMapping("brands/{brandId}")
	public ResponseEntity<List<Board>> boardList(
			@Parameter(description = "브랜드 ID", example = "1") @PathVariable int brandId) {
		List<Board> board = boardService.selectBoard(brandId);
		return ResponseEntity.ok(board != null ? board : List.of());
	}

	/**
	 * Commu-2b 특정 게시판 조회 (브랜드 슬러그) GET /boards/brands/slug/{brandSlug}
	 */
	@Operation(summary = "특정 브랜드 게시글 목록 조회 (슬러그)", description = "브랜드 슬러그에 해당하는 게시글 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (게시글 없으면 빈 배열)")
	@GetMapping("brands/slug/{brandSlug}")
	public ResponseEntity<List<Board>> boardListBySlug(
			@Parameter(description = "브랜드 슬러그", example = "hyundai") @PathVariable String brandSlug) {
		List<Board> board = boardService.selectBoardBySlug(brandSlug);
		return ResponseEntity.ok(board != null ? board : List.of());
	}

	/**
	 * Commu-3 특정 게시글 조회 GET /boards/articles/{articleId}
	 */
	@Operation(summary = "게시글 상세 조회", description = "게시글 ID에 해당하는 게시글을 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
	})
	@GetMapping("articles/{articleId}")
	public ResponseEntity<Board> detailArticle(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId) {
		Board article = boardService.selectArticle(articleId);
		if (article != null) {
			return ResponseEntity.ok(article);
		}
		return ResponseEntity.notFound().build();
	}

	@PostMapping("articles/{articleId}/views")
	public ResponseEntity<Board> increaseArticleViewCount(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId) {
		Board article = boardService.increaseArticleViewCount(articleId);
		if (article != null) {
			return ResponseEntity.ok(article);
		}
		return ResponseEntity.notFound().build();
	}

	/**
	 * Commu-4 게시글 작성 POST /boards/article
	 */
	@Operation(summary = "게시글 작성", description = "새 게시글을 작성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 값 누락 등)")
	})
	@PostMapping(value = "/article", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Board> createArticle(@RequestBody Board board, Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			if (hasTooManyImages(board.getImageUrls())) {
				return ResponseEntity.badRequest().build();
			}

			board.setUserId(userId);
			board.setTestDriveAvailable(Boolean.TRUE.equals(board.getTestDriveAvailable()));
			log.info(
					"[boards-create-json] userId={} brandId={} title={} imageUrlCount={}",
					board.getUserId(),
					board.getBrandId(),
					board.getTitle(),
					board.getImageUrls() == null ? 0 : board.getImageUrls().size());
			boardService.insertArticle(board);
			insertArticleImages(board.getArticleId(), board.getImageUrls());
			log.info("[boards-create-json] created articleId={}", board.getArticleId());
			return ResponseEntity.status(HttpStatus.CREATED).body(board);
		} catch (DataIntegrityViolationException e) {
			log.warn("[boards-create-json] bad request userId={} brandId={}", board.getUserId(), board.getBrandId(), e);
			return ResponseEntity.badRequest().build();
		}
	}

	@PostMapping(value = "/article/form")
	public ResponseEntity<Board> createArticleWithImage(
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam(defaultValue = "false") boolean testDriveAvailable,
			@RequestParam int brandId,
			@RequestParam String boardName,
			@RequestParam(value = "images", required = false) List<MultipartFile> images,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			int imageCount = images == null ? 0 : images.size();
			log.info(
					"[boards-create-form] entered userId={} brandId={} boardName={} title={} imageCount={}",
					userId,
					brandId,
					boardName,
					title,
					imageCount);

			if (imageCount > MAX_ARTICLE_IMAGE_COUNT) {
				return ResponseEntity.badRequest().build();
			}

			if (images != null) {
				for (int index = 0; index < images.size(); index++) {
					MultipartFile image = images.get(index);
					log.info(
							"[boards-create-form] image index={} originalName={} contentType={} size={}",
							index,
							image.getOriginalFilename(),
							image.getContentType(),
							image.getSize());
				}
			}

			Board board = new Board();
			board.setUserId(userId);
			board.setTitle(title);
			board.setContent(content);
			board.setTestDriveAvailable(testDriveAvailable);
			board.setBrandId(brandId);
			board.setBoardName(boardName);

			List<String> imageUrls = new ArrayList<>();
			if (images != null) {
				for (MultipartFile image : images) {
					String objectName = gcsStorageService.uploadArticleImage(image);
					if (objectName != null) {
						String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
								.path("/boards/article-image")
								.queryParam("objectName", objectName)
								.toUriString();
						imageUrls.add(imageUrl);
					}
				}
			}

			boardService.insertArticle(board);
			insertArticleImages(board.getArticleId(), imageUrls);
			board.setImageUrls(imageUrls);
			log.info(
					"[boards-create-form] created articleId={} storedImageCount={}",
					board.getArticleId(),
					imageUrls.size());
			return ResponseEntity.status(HttpStatus.CREATED).body(board);
		} catch (DataIntegrityViolationException | IllegalArgumentException e) {
			log.warn(
					"[boards-create-form] bad request userId={} brandId={} title={}",
					userId,
					brandId,
					title,
					e);
			return ResponseEntity.badRequest().build();
		} catch (IOException e) {
			log.error(
					"[boards-create-form] image upload failed userId={} brandId={} title={}",
					userId,
					brandId,
					title,
					e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("article-image")
	public ResponseEntity<byte[]> getArticleImage(@RequestParam String objectName) {
		StoredImage image = gcsStorageService.downloadImage(objectName);

		if (image == null) {
			return ResponseEntity.notFound().build();
		}

		MediaType contentType = image.contentType() != null
				? MediaType.parseMediaType(image.contentType())
				: MediaType.APPLICATION_OCTET_STREAM;

		return ResponseEntity.ok()
				.contentType(contentType)
				.body(image.content());
	}

	private void insertArticleImages(Integer articleId, List<String> imageUrls) {
		if (articleId == null || imageUrls == null) {
			return;
		}

		for (int index = 0; index < imageUrls.size(); index++) {
			String imageUrl = imageUrls.get(index);
			if (imageUrl != null && !imageUrl.isBlank()) {
				boardService.insertArticleImage(articleId, imageUrl, index);
			}
		}
	}

	private boolean hasTooManyImages(List<String> imageUrls) {
		return imageUrls != null && imageUrls.size() > MAX_ARTICLE_IMAGE_COUNT;
	}

	/**
	 * Commu-5 게시글 수정
	 */
	@Operation(summary = "게시글 수정", description = "게시글 ID에 해당하는 게시글을 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
	})
	@PutMapping("articles/{articleId}")
	public ResponseEntity<Board> updateArticle(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			@RequestBody Board board,
			Authentication authentication) {
		Board searchArticle = boardService.selectArticle(articleId);
		if (searchArticle == null) {
			return ResponseEntity.notFound().build();
		}

		if (!canManageArticle(searchArticle, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		board.setArticleId(articleId);
		board.setTestDriveAvailable(Boolean.TRUE.equals(board.getTestDriveAvailable()));
		boardService.updateArticle(board);
		return ResponseEntity.ok(board);
	}

	@PutMapping("articles/{articleId}/form")
	public ResponseEntity<Board> updateArticleWithImages(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam(defaultValue = "false") boolean testDriveAvailable,
			@RequestParam(value = "imageOrderManaged", defaultValue = "false") boolean imageOrderManaged,
			@RequestParam(value = "imageOrder", required = false) List<String> imageOrder,
			@RequestParam(value = "images", required = false) List<MultipartFile> images,
			Authentication authentication) {
		Board searchArticle = boardService.selectArticle(articleId);
		if (searchArticle == null) {
			return ResponseEntity.notFound().build();
		}

		if (!canManageArticle(searchArticle, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		try {
			int imageCount = images == null ? 0 : images.size();
			List<String> requestedImageOrder = imageOrder == null ? List.of() : imageOrder;
			boolean shouldSyncImages = imageOrderManaged || imageOrder != null;
			log.info(
					"[boards-update-form] entered articleId={} title={} imageCount={} imageOrderManaged={} imageOrderCount={}",
					articleId,
					title,
					imageCount,
					imageOrderManaged,
					requestedImageOrder.size());

			if (imageCount > MAX_ARTICLE_IMAGE_COUNT
					|| (shouldSyncImages && requestedImageOrder.size() > MAX_ARTICLE_IMAGE_COUNT)) {
				return ResponseEntity.badRequest().build();
			}

			Board board = new Board();
			board.setArticleId(articleId);
			board.setTitle(title);
			board.setContent(content);
			board.setTestDriveAvailable(testDriveAvailable);
			boardService.updateArticle(board);

			if (shouldSyncImages) {
				List<String> oldImageUrls = boardService.selectArticleImages(articleId);
				List<String> imageUrls = buildOrderedArticleImageUrls(requestedImageOrder, images, oldImageUrls);
				Set<String> keptImageUrls = new HashSet<>(imageUrls);
				List<String> removedImageUrls = oldImageUrls.stream()
						.filter((imageUrl) -> !keptImageUrls.contains(imageUrl))
						.toList();

				boardService.deleteArticleImages(articleId);
				insertArticleImages(articleId, imageUrls);
				deleteStoredArticleImages(removedImageUrls);
				log.info("[boards-update-form] synced articleId={} imageCount={}", articleId, imageUrls.size());
			} else if (images != null && !images.isEmpty()) {
				List<String> imageUrls = new ArrayList<>();
				for (MultipartFile image : images) {
					log.info(
							"[boards-update-form] image originalName={} contentType={} size={}",
							image.getOriginalFilename(),
							image.getContentType(),
							image.getSize());
					String objectName = gcsStorageService.uploadArticleImage(image);
					if (objectName != null) {
						String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
								.path("/boards/article-image")
								.queryParam("objectName", objectName)
								.toUriString();
						imageUrls.add(imageUrl);
					}
				}

				List<String> oldImageUrls = boardService.selectArticleImages(articleId);
				deleteStoredArticleImages(oldImageUrls);
				boardService.deleteArticleImages(articleId);
				insertArticleImages(articleId, imageUrls);
				log.info("[boards-update-form] replaced articleId={} imageCount={}", articleId, imageUrls.size());
			}

			Board updatedArticle = boardService.selectArticle(articleId);
			return ResponseEntity.ok(updatedArticle);
		} catch (IllegalArgumentException e) {
			log.warn("[boards-update-form] bad request articleId={}", articleId, e);
			return ResponseEntity.badRequest().build();
		} catch (IOException e) {
			log.error("[boards-update-form] image upload failed articleId={}", articleId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private List<String> buildOrderedArticleImageUrls(
			List<String> imageOrder,
			List<MultipartFile> images,
			List<String> currentImageUrls) throws IOException {
		List<MultipartFile> imageFiles = images == null ? List.of() : images;
		Set<String> currentImageUrlSet = new HashSet<>(currentImageUrls == null ? List.of() : currentImageUrls);
		Set<String> usedExistingImageUrls = new HashSet<>();
		Set<Integer> usedNewImageIndexes = new HashSet<>();

		for (String imageOrderItem : imageOrder) {
			if (imageOrderItem == null || imageOrderItem.isBlank()) {
				throw new IllegalArgumentException("Image order item is blank.");
			}

			if (imageOrderItem.startsWith(IMAGE_ORDER_EXISTING_PREFIX)) {
				String imageUrl = imageOrderItem.substring(IMAGE_ORDER_EXISTING_PREFIX.length());

				if (!currentImageUrlSet.contains(imageUrl) || !usedExistingImageUrls.add(imageUrl)) {
					throw new IllegalArgumentException("Invalid existing article image.");
				}
			} else if (imageOrderItem.startsWith(IMAGE_ORDER_NEW_PREFIX)) {
				int imageIndex = parseNewImageIndex(imageOrderItem);

				if (imageIndex < 0 || imageIndex >= imageFiles.size() || !usedNewImageIndexes.add(imageIndex)) {
					throw new IllegalArgumentException("Invalid new article image index.");
				}
			} else {
				throw new IllegalArgumentException("Invalid image order item.");
			}
		}

		if (usedNewImageIndexes.size() != imageFiles.size()) {
			throw new IllegalArgumentException("All uploaded article images must be included in image order.");
		}

		Map<Integer, String> uploadedImageUrlsByIndex = new HashMap<>();
		List<String> imageUrls = new ArrayList<>();

		for (String imageOrderItem : imageOrder) {
			if (imageOrderItem.startsWith(IMAGE_ORDER_EXISTING_PREFIX)) {
				imageUrls.add(imageOrderItem.substring(IMAGE_ORDER_EXISTING_PREFIX.length()));
				continue;
			}

			int imageIndex = parseNewImageIndex(imageOrderItem);
			String imageUrl = uploadedImageUrlsByIndex.get(imageIndex);

			if (imageUrl == null) {
				MultipartFile image = imageFiles.get(imageIndex);
				log.info(
						"[boards-update-form] image originalName={} contentType={} size={}",
						image.getOriginalFilename(),
						image.getContentType(),
						image.getSize());
				String objectName = gcsStorageService.uploadArticleImage(image);

				if (objectName == null) {
					throw new IllegalArgumentException("Uploaded article image is empty.");
				}

				imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
						.path("/boards/article-image")
						.queryParam("objectName", objectName)
						.toUriString();
				uploadedImageUrlsByIndex.put(imageIndex, imageUrl);
			}

			imageUrls.add(imageUrl);
		}

		return imageUrls;
	}

	private int parseNewImageIndex(String imageOrderItem) {
		try {
			return Integer.parseInt(imageOrderItem.substring(IMAGE_ORDER_NEW_PREFIX.length()));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid new article image index.", e);
		}
	}

	private void deleteStoredArticleImages(List<String> imageUrls) {
		if (imageUrls == null) {
			return;
		}

		for (String imageUrl : imageUrls) {
			String objectName = extractObjectName(imageUrl);
			if (objectName != null) {
				try {
					gcsStorageService.deleteImage(objectName);
				} catch (RuntimeException e) {
					log.warn("[boards-image-delete] failed objectName={}", objectName, e);
				}
			}
		}
	}

	private String extractObjectName(String imageUrl) {
		if (imageUrl == null || !imageUrl.contains("/boards/article-image") || !imageUrl.contains("objectName=")) {
			return null;
		}

		String query = imageUrl.substring(imageUrl.indexOf('?') + 1);
		for (String parameter : query.split("&")) {
			String[] keyValue = parameter.split("=", 2);
			if (keyValue.length == 2 && "objectName".equals(keyValue[0])) {
				try {
					return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					return keyValue[1];
				}
			}
		}

		return null;
	}

	/**
	 * Commu-6 게시글 삭제
	 */
	@Operation(summary = "게시글 삭제", description = "게시글 ID에 해당하는 게시글을 삭제합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
	})
	@DeleteMapping("articles/{articleId}")
	public ResponseEntity<Void> deleteArticle(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			Authentication authentication) {
		Board searchArticle = boardService.selectArticle(articleId);
		if (searchArticle == null) {
			return ResponseEntity.notFound().build();
		}

		if (!canManageArticle(searchArticle, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		List<String> imageUrls = searchArticle.getImageUrls();
		boardService.deleteArticle(articleId);
		deleteStoredArticleImages(imageUrls);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Commu-7 게시글 좋아요 상태 조회 GET /boards/articles/{articleId}/likes
	 */
	@Operation(summary = "게시글 좋아요 상태 조회", description = "게시글의 좋아요 수와 요청 사용자의 좋아요 여부를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@GetMapping("articles/{articleId}/likes")
	public ResponseEntity<ArticleLikeStatus> articleLikeStatus(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			Authentication authentication) {
		return ResponseEntity.ok(boardService.getArticleLikeStatus(articleId, currentUserId(authentication)));
	}

	/**
	 * Commu-8 게시글 좋아요 등록 POST /boards/articles/{articleId}/likes
	 */
	@Operation(summary = "게시글 좋아요 등록", description = "게시글에 좋아요를 등록합니다.")
	@ApiResponse(responseCode = "200", description = "등록 성공")
	@PostMapping("articles/{articleId}/likes")
	public ResponseEntity<ArticleLikeStatus> likeArticle(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(boardService.likeArticle(articleId, userId));
	}

	/**
	 * Commu-9 게시글 좋아요 취소 DELETE /boards/articles/{articleId}/likes
	 */
	@Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
	@ApiResponse(responseCode = "200", description = "취소 성공")
	@DeleteMapping("articles/{articleId}/likes")
	public ResponseEntity<ArticleLikeStatus> unlikeArticle(
			@Parameter(description = "게시글 ID", example = "1") @PathVariable int articleId,
			Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(boardService.unlikeArticle(articleId, userId));
	}

	/**
	 * Commu-10 사용자가 작성한 글 목록 GET /boards/users/{userId}/articles
	 */
	@Operation(summary = "내가 작성한 글 목록", description = "사용자가 작성한 게시글 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 배열)")
	@GetMapping("users/{userId}/articles")
	public ResponseEntity<List<Board>> userArticles(
			@Parameter(description = "사용자 ID", example = "ssafy") @PathVariable String userId,
			Authentication authentication) {
		if (!canAccessUser(userId, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		List<Board> articles = boardService.selectArticlesByUser(userId);
		return ResponseEntity.ok(articles != null ? articles : List.of());
	}

	/**
	 * Commu-11 사용자가 좋아요한 글 목록 GET /boards/users/{userId}/liked-articles
	 */
	@Operation(summary = "내가 좋아요한 글 목록", description = "사용자가 좋아요한 게시글 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 배열)")
	@GetMapping("users/{userId}/liked-articles")
	public ResponseEntity<List<Board>> userLikedArticles(
			@Parameter(description = "사용자 ID", example = "ssafy") @PathVariable String userId,
			Authentication authentication) {
		if (!canAccessUser(userId, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		List<Board> articles = boardService.selectLikedArticlesByUser(userId);
		return ResponseEntity.ok(articles != null ? articles : List.of());
	}

	/**
	 * Commu-12 사용자가 댓글을 단 글 목록 GET /boards/users/{userId}/commented-articles
	 */
	@Operation(summary = "내가 댓글 단 글 목록", description = "사용자가 댓글을 작성한 게시글 목록을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 배열)")
	@GetMapping("users/{userId}/commented-articles")
	public ResponseEntity<List<Board>> userCommentedArticles(
			@Parameter(description = "사용자 ID", example = "ssafy") @PathVariable String userId,
			Authentication authentication) {
		if (!canAccessUser(userId, authentication)) {
			return ResponseEntity.status(ownerErrorStatus(authentication)).build();
		}

		List<Board> articles = boardService.selectCommentedArticlesByUser(userId);
		return ResponseEntity.ok(articles != null ? articles : List.of());
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

	private boolean canManageArticle(Board article, Authentication authentication) {
		String userId = currentUserId(authentication);
		return userId != null && (userId.equals(article.getUserId()) || isAdmin(authentication));
	}

	private boolean canAccessUser(String userId, Authentication authentication) {
		String currentUserId = currentUserId(authentication);
		return currentUserId != null && (currentUserId.equals(userId) || isAdmin(authentication));
	}

	private HttpStatus ownerErrorStatus(Authentication authentication) {
		return currentUserId(authentication) == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
	}
}
