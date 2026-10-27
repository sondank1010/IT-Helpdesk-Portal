package com.example.backend.api;

import com.example.backend.service.AuthService;
import com.example.backend.service.AuthService.Tokens;
import com.example.backend.service.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public record RegisterRequest(@Email @NotBlank @Size(max = 190) String email,
                                  @NotBlank @Size(min = 8) String password,
                                  @NotBlank @Size(max = 255) String fullName) { }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }

    public record RefreshRequest(@NotBlank String sessionId, @NotBlank String refreshToken) { }

    @Operation(summary = "Đăng ký tài khoản vai trò USER", responses = {
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")})
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.email(), request.password(), request.fullName());
    }

    @Operation(summary = "Đăng nhập, nhận access token và refresh token",
            description = "Access token có hạn 15 phút, phiên refresh token có hạn 7 ngày. Dùng accessToken cho nút Authorize.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cặp token và sessionId"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")})
    @SecurityRequirements
    @PostMapping("/login")
    public Tokens login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @Operation(summary = "Đổi refresh token lấy cặp token mới", description = "Gửi sessionId và refreshToken từ lần đăng nhập/refresh gần nhất. Refresh token cũ mất hiệu lực sau khi đổi.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cặp token mới và sessionId"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")})
    @SecurityRequirements
    @PostMapping("/refresh-token")
    public Tokens refresh(@Valid @RequestBody RefreshRequest request) {
        return auth.refresh(request.sessionId(), request.refreshToken());
    }

    @Operation(summary = "Đăng xuất, thu hồi phiên hiện tại", responses = {
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")})
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        auth.logout(CurrentUser.sessionId());
    }

    @Operation(summary = "Xem tài khoản đang đăng nhập", responses = {
            @ApiResponse(responseCode = "200", description = "Thông tin tài khoản hiện tại"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")})
    @GetMapping("/me")
    public UserView me() {
        return UserView.of(CurrentUser.get());
    }
}
