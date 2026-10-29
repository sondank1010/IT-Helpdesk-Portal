package com.example.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tài liệu OpenAPI tại /v3/api-docs, giao diện Swagger tại /swagger-ui.html. */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "IT Helpdesk API", version = "1.0",
                description = "Quản lý ticket hỗ trợ IT: xác thực, hồ sơ, ticket, danh mục. "
                        + "Đăng nhập để lấy accessToken, sau đó nhập token vào nút Authorize."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    public static final String BEARER = "bearerAuth";

    /** Các controller tham chiếu lại mô tả lỗi, không thay đổi cách xử lý lỗi thực tế. */
    @Bean
    OpenAPI helpdeskOpenApi() {
        return new OpenAPI().components(new Components()
                .addSchemas("ErrorResponse", new ObjectSchema()
                        .addProperty("error", new StringSchema().example("Thông báo lỗi"))
                        .addRequiredItem("error"))
                .addResponses("BadRequest", errorResponse("Dữ liệu không hợp lệ hoặc vi phạm quy tắc nghiệp vụ"))
                .addResponses("Unauthorized", errorResponse("Chưa đăng nhập, sai thông tin đăng nhập hoặc token hết hạn/không hợp lệ"))
                .addResponses("Forbidden", errorResponse("Không có quyền thực hiện thao tác"))
                .addResponses("NotFound", errorResponse("Không tìm thấy tài nguyên"))
                .addResponses("Conflict", errorResponse("Dữ liệu trùng lặp hoặc tài nguyên đang được sử dụng")));
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/json", new MediaType()
                        .schema(new ObjectSchema().$ref("#/components/schemas/ErrorResponse"))));
    }
}
