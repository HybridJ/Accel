package com.accel.api.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.accel.api.security.JwtTokenProvider;
import com.accel.api.storage.GcsStorageService;
import com.accel.api.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthDao authDao;
	private final PasswordEncoder passwordEncoder;
	private final GcsStorageService gcsStorageService;
	private final JwtDecoder jwtDecoder;

	@Override
	public int signUp(User user, MultipartFile profileImage) throws IOException {
		// 회원 정보 수정 API와 동일한 나이 검증을 적용해 비정상 값(예: 1234) 저장을 막는다.
		if (user.getAge() != null && (user.getAge() < 0 || user.getAge() > 150)) {
			throw new IllegalArgumentException("Age must be between 0 and 150.");
		}

		if (authDao.findByUserId(user.getUserId()) != null) {
			throw new DuplicateKeyException("Duplicate userId: " + user.getUserId());
		}

		String profileImg = gcsStorageService.uploadProfileImage(profileImage);

		if (StringUtils.hasText(profileImg)) {
			user.setProfileImg(profileImg);
		} else if (!StringUtils.hasText(user.getProfileImg())) {
			user.setProfileImg("profile_default.png");
		}

		user.setRole("USER");
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return authDao.signUp(user);
	}

	@Override
	public AuthTokens login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
		);

		String token = jwtTokenProvider.generateAccessToken(authentication);
		String role = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.findFirst().orElse("ROLE_USER");

		return issueTokens(authentication.getName(), role, token);
	}

	@Override
	public AuthTokens refresh(String refreshToken) {
		Jwt jwt = decodeRefreshToken(refreshToken);
		String tokenHash = hashToken(refreshToken);
		RefreshToken storedToken = authDao.findRefreshToken(tokenHash);

		if (storedToken == null
				|| storedToken.isRevoked()
				|| storedToken.getExpiresAt() == null
				|| storedToken.getExpiresAt().isBefore(LocalDateTime.now())
				|| !storedToken.getUserId().equals(jwt.getSubject())) {
			throw new BadCredentialsException("Invalid refresh token.");
		}

		User user = authDao.findByUserId(jwt.getSubject());
		if (user == null) {
			throw new BadCredentialsException("User not found.");
		}

		authDao.revokeRefreshToken(tokenHash);
		String role = normalizeRole(user.getRole());
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), role);
		return issueTokens(user.getUserId(), role, accessToken);
	}

	@Override
	public void logout(String refreshToken, String accessToken) {
		if (!StringUtils.hasText(refreshToken)) {
			revokeAccessToken(accessToken);
			return;
		}

		authDao.revokeRefreshToken(hashToken(refreshToken));
		revokeAccessToken(accessToken);
	}

	private AuthTokens issueTokens(String userId, String role, String accessToken) {
		String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
		long refreshMaxAgeSeconds = Duration.ofMillis(jwtTokenProvider.getRefreshExpirationMs()).toSeconds();

		authDao.insertRefreshToken(
				hashToken(refreshToken),
				userId,
				LocalDateTime.now().plusSeconds(refreshMaxAgeSeconds));

		return new AuthTokens(
				new LoginResponse(accessToken, "Bearer", userId, normalizeRole(role)),
				refreshToken,
				refreshMaxAgeSeconds);
	}

	private Jwt decodeRefreshToken(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw new BadCredentialsException("Refresh token is required.");
		}

		try {
			Jwt jwt = jwtDecoder.decode(refreshToken);
			if (!"refresh".equals(jwt.getClaimAsString("tokenType"))) {
				throw new BadCredentialsException("Invalid token type.");
			}
			return jwt;
		} catch (JwtException e) {
			throw new BadCredentialsException("Invalid refresh token.", e);
		}
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available.", e);
		}
	}

	private void revokeAccessToken(String accessToken) {
		if (!StringUtils.hasText(accessToken)) {
			return;
		}

		try {
			Jwt jwt = jwtDecoder.decode(accessToken);
			if (!"access".equals(jwt.getClaimAsString("tokenType"))
					|| !StringUtils.hasText(jwt.getId())
					|| jwt.getExpiresAt() == null
					|| jwt.getExpiresAt().isBefore(Instant.now())) {
				return;
			}

			authDao.insertRevokedAccessToken(
					jwt.getId(),
					LocalDateTime.ofInstant(jwt.getExpiresAt(), ZoneId.systemDefault()));
		} catch (JwtException | BadCredentialsException e) {
			// Logout should still clear the refresh cookie even when the access token is already invalid.
		}
	}

	private String normalizeRole(String role) {
		if (!StringUtils.hasText(role)) {
			return "ROLE_USER";
		}

		String normalizedRole = role.trim().toUpperCase();
		return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
	}
}
