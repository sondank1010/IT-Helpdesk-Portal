package com.example.backend.api;

import com.example.helpdesk.service.AuthService;
import com.example.helpdesk.service.AuthService.Tokens;
import com.example.helpdesk.service.UserView;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Đăng ký tài khoản vai trò USER")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.email(), request.password(), request.fullName());
    }

    @Operation(summary = "Đăng nhập, nhận access token và refresh token")
    @SecurityRequirements
    @PostMapping("/login")
    public Tokens login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @Operation(summary = "Đổi refresh token lấy cặp token mới")
    @SecurityRequirements
    @PostMapping("/refresh-token")
    public Tokens refresh(@Valid @RequestBody RefreshRequest request) {
        return auth.refresh(request.sessionId(), request.refreshToken());
    }

    @Operation(summary = "Đăng xuất, thu hồi phiên hiện tại")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        auth.logout(CurrentUser.sessionId());
    }

    @Operation(summary = "Xem tài khoản đang đăng nhập")
    @GetMapping("/me")
    public UserView me() {
        return UserView.of(CurrentUser.get());
    }
}
