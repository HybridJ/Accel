package com.accel.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS는 SecurityConfig.corsConfigurationSource() 한 곳에서만 관리한다.
// (과거 이 클래스의 addCorsMappings가 localhost:5173만 허용해, Security CORS와
//  이중으로 동작하며 운영 도메인 요청을 403으로 막을 위험이 있어 제거함.)
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
