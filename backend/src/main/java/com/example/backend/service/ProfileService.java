package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository users;
    private final PasswordHasher passwords;

    public ProfileService(UserRepository users, PasswordHasher passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    /** Sửa tên và/hoặc đổi mật khẩu. Đổi mật khẩu bắt buộc nhập đúng mật khẩu hiện tại. */
    @Transactional
    public UserView update(Long userId, String fullName, String currentPassword, String newPassword) {
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("User not found"));
        if (fullName != null) {
            if (fullName.isBlank()) {
                throw BusinessException.invalid("fullName must not be blank");
            }
            user.setFullName(fullName.trim());
        }
        if (newPassword != null) {
            if (currentPassword == null || !passwords.matches(currentPassword, user.getPasswordHash())) {
                throw BusinessException.invalid("currentPassword is incorrect");
            }
            user.setPasswordHash(passwords.hash(newPassword));
        }
        return UserView.of(user);
    }
}
