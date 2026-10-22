package com.example.backend.service;

import com.example.backend.model.Role;
import com.example.backend.model.User;

public record UserView(Long id, String email, String fullName, Role role) {

    public static UserView of(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
