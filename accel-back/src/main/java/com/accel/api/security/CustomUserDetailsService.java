package com.accel.api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.accel.api.auth.AuthDao;
import com.accel.api.user.User;

import lombok.RequiredArgsConstructor;

/**
 * '사용자가 누구인가'(UserDetails) 를 반환하는 Spring Security 표준 인터페이스 구현체.
 * 로그인 시 AuthenticationManager 가 이 서비스를 호출해 사용자를 찾고,
 * 반환된 UserDetails 의 password(BCrypt 해시) 와 입력 비밀번호를 내부적으로 비교한다.
 * 02 JWT 강의의 구현을 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthDao authDao;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = authDao.findByUserId(userId);
        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }

        // ② UserDetails 로 변환 — roles("USER") 가 자동으로 "ROLE_USER" prefix 를 붙인다.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPassword()) // BCrypt 해시
                .roles(user.getRole())         // "USER" -> ROLE_USER, "ADMIN" -> ROLE_ADMIN
                .build();
    }
}
