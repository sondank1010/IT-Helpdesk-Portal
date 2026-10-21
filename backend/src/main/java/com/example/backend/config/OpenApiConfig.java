package com.example.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Tài liệu OpenAPI tại /v3/api-docs, giao diện Swagger tại /swagger-ui.html. */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "IT Helpdesk API", version = "1.0",
                description = "Quản lý ticket hỗ trợ IT: xác thực, ticket, danh mục."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    public static final String BEARER = "bearerAuth";
}
