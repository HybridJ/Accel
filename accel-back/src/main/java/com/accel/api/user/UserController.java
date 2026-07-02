package com.accel.api.user;

import java.io.IOException;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.accel.api.board.Board;
import com.accel.api.storage.GcsStorageService;
import com.accel.api.storage.GcsStorageService.StoredImage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "마이페이지 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final GcsStorageService gcsStorageService;

	@Operation(summary = "내 마이페이지 조회", description = "JWT subject 기준으로 로그인한 본인의 회원 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "회원 없음")
	})
	@GetMapping("/me")
	public ResponseEntity<User> getMyPage(Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return getMyPageByUserId(userId);
	}

	@Operation(summary = "내 마이페이지 조회", description = "경로의 userId가 JWT subject와 같을 때만 회원 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 정보가 아님"),
		@ApiResponse(responseCode = "404", description = "회원 없음")
	})
	@GetMapping("/me/{userId}")
	public ResponseEntity<User> getMyPage(@PathVariable String userId, Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return getMyPageByUserId(userId);
	}

	@Operation(summary = "내 마이페이지 수정", description = "JWT subject 기준으로 본인의 이름, 닉네임, 나이, 이메일을 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "회원 없음"),
		@ApiResponse(responseCode = "409", description = "닉네임 중복")
	})
	@PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<User> updateMyPage(@RequestBody UserUpdateRequest request, Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return updateMyPageByUserId(userId, request);
	}

	@Operation(summary = "내 마이페이지 수정", description = "경로의 userId가 JWT subject와 같을 때만 본인의 회원 정보를 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 정보가 아님"),
		@ApiResponse(responseCode = "404", description = "회원 없음"),
		@ApiResponse(responseCode = "409", description = "닉네임 중복")
	})
	@PutMapping(value = "/me/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<User> updateMyPage(@PathVariable String userId,
			@RequestBody UserUpdateRequest request, Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return updateMyPageByUserId(userId, request);
	}

	@PutMapping(value = "/me/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<User> updateMyPageWithProfileImage(
			@PathVariable String userId,
			@ModelAttribute UserUpdateRequest request,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
			Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return updateMyPageByUserId(userId, request, profileImage);
	}

	@PostMapping(value = "/me/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<User> updateProfileImage(
			@PathVariable String userId,
			@RequestPart("profileImage") MultipartFile profileImage,
			Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return updateProfileImageByUserId(userId, profileImage);
	}

	@Operation(summary = "회원 탈퇴", description = "JWT subject 기준으로 본인 계정과 연관된 게시글, 댓글, 좋아요를 함께 삭제합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "탈퇴 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMyPage(Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		userService.deleteMyPage(userId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원 탈퇴", description = "경로의 userId가 JWT subject와 같을 때만 본인 계정을 삭제합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "탈퇴 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 계정이 아님")
	})
	@DeleteMapping("/me/{userId}")
	public ResponseEntity<Void> deleteMyPage(@PathVariable String userId, Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		userService.deleteMyPage(userId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "내 작성 글 조회", description = "JWT subject 기준으로 본인이 작성한 게시글 목록을 최신순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@GetMapping("/me/myboards")
	public ResponseEntity<List<Board>> getMyBoards(Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(userService.getMyBoards(userId));
	}

	@Operation(summary = "내 작성 글 조회", description = "경로의 userId가 JWT subject와 같을 때만 작성 글을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 정보가 아님")
	})
	@GetMapping("/me/{userId}/myboards")
	public ResponseEntity<List<Board>> getMyBoards(@PathVariable String userId, Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return ResponseEntity.ok(userService.getMyBoards(userId));
	}

	@Operation(summary = "내 댓글 단 글 조회", description = "JWT subject 기준으로 본인이 댓글을 단 게시글 목록을 중복 없이 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@GetMapping("/me/mycomments")
	public ResponseEntity<List<Board>> getMyCommentedBoards(Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(userService.getMyCommentedBoards(userId));
	}

	@Operation(summary = "내 댓글 단 글 조회", description = "경로의 userId가 JWT subject와 같을 때만 댓글 단 글을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 정보가 아님")
	})
	@GetMapping("/me/{userId}/mycomments")
	public ResponseEntity<List<Board>> getMyCommentedBoards(@PathVariable String userId,
			Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return ResponseEntity.ok(userService.getMyCommentedBoards(userId));
	}

	@Operation(summary = "내 좋아요 글 조회", description = "JWT subject 기준으로 본인이 좋아요한 게시글 목록을 최신순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@GetMapping("/me/mylikes")
	public ResponseEntity<List<Board>> getMyLikedBoards(Authentication authentication) {
		String userId = currentUserId(authentication);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(userService.getMyLikedBoards(userId));
	}

	@Operation(summary = "내 좋아요 글 조회", description = "경로의 userId가 JWT subject와 같을 때만 좋아요한 글을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "403", description = "본인 정보가 아님")
	})
	@GetMapping("/me/{userId}/mylikes")
	public ResponseEntity<List<Board>> getMyLikedBoards(@PathVariable String userId, Authentication authentication) {
		HttpStatus errorStatus = ownerErrorStatus(authentication, userId);
		if (errorStatus != null) {
			return ResponseEntity.status(errorStatus).build();
		}
		return ResponseEntity.ok(userService.getMyLikedBoards(userId));
	}

	@Operation(summary = "다른 유저 공개 프로필 조회", description = "닉네임, 프로필 이미지, 작성 글 목록을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "회원 없음")
	})
	@GetMapping("/{userId}/profile")
	public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
		UserProfileResponse profile = userService.getUserProfile(userId);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(profile);
	}

	@GetMapping("/{userId}/profile-image")
	public ResponseEntity<byte[]> getUserProfileImage(@PathVariable String userId) {
		User user = userService.getById(userId);

		if (user == null) {
			return ResponseEntity.notFound().build();
		}

		StoredImage image = gcsStorageService.downloadProfileImage(user.getProfileImg());

		if (image == null) {
			return ResponseEntity.notFound().build();
		}

		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

		if (image.contentType() != null) {
			mediaType = MediaType.parseMediaType(image.contentType());
		}

		return ResponseEntity.ok()
				.contentType(mediaType)
				.cacheControl(CacheControl.noCache())
				.body(image.content());
	}

	private ResponseEntity<User> getMyPageByUserId(String userId) {
		User user = userService.getMyPage(userId);
		if (user == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(user);
	}

	private ResponseEntity<User> updateMyPageByUserId(String userId, UserUpdateRequest request) {
		try {
			User updated = userService.updateMyPage(userId, request);
			if (updated == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(updated);
		} catch (DuplicateNicknameException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	private ResponseEntity<User> updateMyPageByUserId(String userId, UserUpdateRequest request,
			MultipartFile profileImage) {
		try {
			User updated = userService.updateMyPage(userId, request, profileImage);
			if (updated == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(updated);
		} catch (DuplicateNicknameException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		} catch (IllegalStateException | IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private ResponseEntity<User> updateProfileImageByUserId(String userId, MultipartFile profileImage) {
		try {
			User updated = userService.updateProfileImage(userId, profileImage);
			if (updated == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		} catch (IllegalStateException | IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private String currentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		return authentication.getName();
	}

	private HttpStatus ownerErrorStatus(Authentication authentication, String userId) {
		String currentUserId = currentUserId(authentication);
		if (currentUserId == null) {
			return HttpStatus.UNAUTHORIZED;
		}
		return currentUserId.equals(userId) ? null : HttpStatus.FORBIDDEN;
	}
}
