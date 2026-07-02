package com.accel.api.auth;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.accel.api.user.User;

@Mapper
public interface AuthDao {
	
	int signUp(User user);
	
	User findByUserId(@Param("userId") String userId);

	int insertRefreshToken(
			@Param("tokenHash") String tokenHash,
			@Param("userId") String userId,
			@Param("expiresAt") LocalDateTime expiresAt);

	RefreshToken findRefreshToken(@Param("tokenHash") String tokenHash);

	int revokeRefreshToken(@Param("tokenHash") String tokenHash);

	int revokeRefreshTokensByUserId(@Param("userId") String userId);

	int insertRevokedAccessToken(
			@Param("jwtId") String jwtId,
			@Param("expiresAt") LocalDateTime expiresAt);

	int existsActiveRevokedAccessToken(@Param("jwtId") String jwtId);
}
