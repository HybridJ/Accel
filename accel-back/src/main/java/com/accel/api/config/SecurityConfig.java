package com.accel.api.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.accel.api.auth.AuthDao;

// JWT ?몄쬆 ?멸? ?ㅼ젙 ?뚯씪
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Value("${jwt.secret}")
	private String secret;

	// 운영 도메인을 포함한 CORS 허용 오리진. 콤마 구분, 환경변수(CORS_ALLOWED_ORIGINS)로 override 가능.
	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;

	private SecretKey secretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
	
	@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/login",
                                "/auth/signup",
                                "/auth/refresh",
                                "/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/logout")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/boards/brands",
                                "/boards/brands/**",
                                "/boards/articles/*",
                                "/boards/articles/*/likes",
                                "/boards/article-image",
                                "/comments/*",
                                "/comments/*/likes",
                                "/user/*/profile",
                                "/user/*/profile-image",
                                "/videos/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/boards/articles/*/views")
                        .permitAll()
                        .requestMatchers(
                                "/ai/**",
                                "/ev/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(publicEndpointAwareBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private BearerTokenResolver publicEndpointAwareBearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

        return request -> isPublicAnonymousEndpoint(request)
                ? null
                : delegate.resolve(request);
    }

    private boolean isPublicAnonymousEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/ai")
                || path.startsWith("/ai/")
                || path.equals("/ev")
                || path.startsWith("/ev/");
    }

	@Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** ?좏겙???쒕챸쨌留뚮즺 寃利?(Resource Server 媛 ?먮룞 ?몄텧) ???댁쟾 JWTUtil.validateToken ???먮━ */
    @Bean
    public JwtDecoder jwtDecoder(AuthDao authDao) {
        JwtDecoder delegate = NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
        return token -> {
            var jwt = delegate.decode(token);
            String tokenType = jwt.getClaimAsString("tokenType");
            String jwtId = jwt.getId();

            if ("access".equals(tokenType)
                    && jwtId != null
                    && authDao.existsActiveRevokedAccessToken(jwtId) > 0) {
                throw new JwtException("Revoked access token.");
            }

            return jwt;
        };
    }

    /** ?좏겙 諛쒓툒???몄퐫?????댁쟾 JWTUtil ???쒕챸 ?꾧뎄???대떦 */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    /** CORS ???꾨줎?몄뿏??Vite 5173) ?곕룞?? ?쒓났. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    String role = jwt.getClaimAsString("role");
                    return role == null || role.isBlank()
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority(role));
                }
                );
        
        return converter;
    }

    
    @Bean
    //  ??븷 怨꾩링 ??ROLE_ADMIN ? ROLE_USER ??沅뚰븳??紐⑤몢 ?ы븿?쒕떎.
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
        		.role("ADMIN").implies("USER").build(); 
    }
}

