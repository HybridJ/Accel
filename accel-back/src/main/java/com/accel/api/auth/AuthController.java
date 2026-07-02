package com.accel.api.auth;

import java.io.IOException;
import java.time.Duration;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.accel.api.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "Authentication API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	@Value("${auth.refresh-cookie.path:/}")
	private String refreshCookiePath;

	@Value("${auth.refresh-cookie.secure:false}")
	private boolean refreshCookieSecure;

	@Value("${auth.refresh-cookie.same-site:Lax}")
	private String refreshCookieSameSite;

	@Operation(summary = "Sign up", description = "Creates a user and uploads an optional profile image.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Created"),
		@ApiResponse(responseCode = "400", description = "Invalid profile image"),
		@ApiResponse(responseCode = "409", description = "Duplicated user id"),
		@ApiResponse(responseCode = "500", description = "Profile image upload failed")
	})
	@PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<User> signUp(
			@ModelAttribute User user,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
		try {
			authService.signUp(user, profileImage);
			user.setPassword(null);
			return ResponseEntity.status(HttpStatus.CREATED).body(user);
		} catch (DuplicateKeyException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Login", description = "Authenticates with user id and password.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Authenticated"),
		@ApiResponse(responseCode = "401", description = "Invalid credentials")
	})
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		AuthTokens tokens = authService.login(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens).toString())
				.body(tokens.loginResponse());
	}

	@Operation(summary = "Refresh", description = "Issues a new access token using the refresh token cookie.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Refreshed"),
		@ApiResponse(responseCode = "401", description = "Invalid refresh token")
	})
	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
		try {
			AuthTokens tokens = authService.refresh(refreshToken);
			return ResponseEntity.ok()
					.header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens).toString())
					.body(tokens.loginResponse());
		} catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie().toString())
					.build();
		}
	}

	@Operation(summary = "Logout", description = "Invalidates the current refresh token.")
	@ApiResponse(responseCode = "200", description = "Logged out")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
			@RequestHeader(name = "X-Access-Token", required = false) String accessToken) {
		authService.logout(refreshToken, accessToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie().toString())
				.build();
	}

	@GetMapping("/logout")
	public ResponseEntity<Void> logoutByGet(
			@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
			@RequestHeader(name = "X-Access-Token", required = false) String accessToken) {
		return logout(refreshToken, accessToken);
	}

	private ResponseCookie refreshTokenCookie(AuthTokens tokens) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokens.refreshToken())
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite(refreshCookieSameSite)
				.path(refreshCookiePath)
				.maxAge(Duration.ofSeconds(tokens.refreshMaxAgeSeconds()))
				.build();
	}

	private ResponseCookie expiredRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite(refreshCookieSameSite)
				.path(refreshCookiePath)
				.maxAge(Duration.ZERO)
				.build();
	}

}
