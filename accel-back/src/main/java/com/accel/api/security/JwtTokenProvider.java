package com.accel.api.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	
	private final JwtEncoder encoder;
	
	@Value("${jwt.expiration}")
	private long expirationMs;

	@Value("${jwt.refresh-expiration:1209600000}")
	private long refreshExpirationMs;
	
	public String generateToken(Authentication authentication) {
		return generateAccessToken(authentication);
	}

	public String generateAccessToken(Authentication authentication) {
		Instant now = Instant.now();
		
		String role = authentication.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.filter(authority -> authority.startsWith("ROLE_"))
					.findFirst().orElse("ROLE_USER");

		return generateJwt(authentication.getName(), role, "access", expirationMs, now);
	}

	public String generateAccessToken(String userId, String role) {
		return generateJwt(userId, normalizeRole(role), "access", expirationMs, Instant.now());
	}

	public String generateRefreshToken(String userId) {
		return generateJwt(userId, null, "refresh", refreshExpirationMs, Instant.now());
	}

	public long getRefreshExpirationMs() {
		return refreshExpirationMs;
	}

	private String generateJwt(String userId, String role, String tokenType, long expirationMillis, Instant now) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("Accel")
				.issuedAt(now)
				.expiresAt(now.plus(expirationMillis, ChronoUnit.MILLIS))
				.subject(userId)
				.id(UUID.randomUUID().toString())
				.claim("tokenType", tokenType)
				.claims(claimsMap -> {
					if (role != null) {
						claimsMap.put("role", role);
					}
				})
				.build();
		
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private String normalizeRole(String role) {
		if (role == null || role.isBlank()) {
			return "ROLE_USER";
		}

		String normalizedRole = role.trim().toUpperCase();
		return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
	}
}
