package com.example.backend.api;

import com.example.backend.service.ProfileService;
import com.example.backend.service.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/users/profile")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    public record ProfileUpdate(@Size(max = 255) String fullName, String currentPassword,
                                @Size(min = 8) String newPassword) { }

    @Operation(summary = "Sửa tên hoặc đổi mật khẩu của tài khoản hiện tại")
    @PutMapping
    public UserView update(@Valid @RequestBody ProfileUpdate request) {
        return profiles.update(CurrentUser.get().getId(), request.fullName(), request.currentPassword(),
                request.newPassword());
    }
}
