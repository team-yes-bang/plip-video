package com.plip.video.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class SwaggerConfig {

	public static final String BEARER_AUTH_SCHEME = "bearerAuth";
	public static final String USER_UUID_SCHEME = "userUuid";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Video Service API")
						.description("5초 영상·이미지(미디어) 업로드/등록/다운로드 가공 API")
						.version("v1.0.0"))
				.components(new Components()
						.addSecuritySchemes(
								BEARER_AUTH_SCHEME,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description(
												"Gateway JWT. 클라이언트는 Bearer를 전달하고, "
														+ "Gateway가 검증 후 X-User-UUID를 주입합니다."
										)
						)
						.addSecuritySchemes(
								USER_UUID_SCHEME,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.HEADER)
										.name("X-User-UUID")
										.description("video 서비스 직접 호출 시 로그인 사용자 UUID. 없으면 401.")
						));
	}
}
