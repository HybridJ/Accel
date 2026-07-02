package com.accel.api.config;  // ← 본인 패키지로 변경

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// SwaggerConfig 설정 파일 (나주원)

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("Accel REST API 명세서")
                        .description("Accel API")
                        .version("v1.0.1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                // JWT Bearer 보안 스킴 등록 (Swagger UI Authorize 버튼)
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
