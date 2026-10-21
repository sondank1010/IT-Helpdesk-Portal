package com.example.backend.service;

import com.example.helpdesk.model.LoginSession;
import com.example.helpdesk.model.Role;
import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.LoginSessionRepository;
import com.example.helpdesk.repository.UserRepository;
import com.example.helpdesk.security.JwtService;
import com.example.helpdesk.security.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final UserRepository users;
    private final LoginSessionRepository sessions;
    private final PasswordHasher passwords;
    private final JwtService jwt;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, LoginSessionRepository sessions, PasswordHasher passwords, JwtService jwt) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public record Tokens(String accessToken, String refreshToken, String sessionId) { }

    public record Authenticated(User user, String sessionId) { }

    @Transactional
    public UserView register(String email, String password, String fullName) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw BusinessException.conflict("Email already registered");
        }
        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(passwords.hash(password));
        user.setFullName(fullName.trim());
        user.setRole(Role.USER);
        return UserView.of(users.save(user));
    }

    @Transactional
    public Tokens login(String email, String password) {
        User user = users.findByEmail(email.trim().toLowerCase())
                .filter(u -> passwords.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> BusinessException.unauthorized("Invalid credentials"));

        LoginSession session = new LoginSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(REFRESH_TOKEN_TTL));
        String refreshToken = randomToken();
        session.setRefreshHash(sha256(refreshToken));
        sessions.save(session);
        return new Tokens(jwt.issue(user.getId(), session.getId()), refreshToken, session.getId());
    }

    /** Cấp cặp token mới => Refresh token cũ mất hiệu lực. */
    @Transactional
    public Tokens refresh(String sessionId, String refreshToken) {
        LoginSession session = sessions.findById(sessionId)
                .filter(s -> s.isActive(Instant.now()))
                .filter(s -> MessageDigest.isEqual(bytes(s.getRefreshHash()), bytes(sha256(refreshToken))))
                .orElseThrow(() -> BusinessException.unauthorized("Invalid refresh token"));
        String next = randomToken();
        session.setRefreshHash(sha256(next));
        return new Tokens(jwt.issue(session.getUser().getId(), sessionId), next, sessionId);
    }

    @Transactional
    public void logout(String sessionId) {
        sessions.findById(sessionId).ifPresent(s -> s.setRevoked(true));
    }

    /** check access token và phiên login; Bỏ phiên logout,hết hạn. */
    @Transactional(readOnly = true)
    public Authenticated authenticate(String accessToken) {
        JwtService.Claims claims = jwt.verify(accessToken);
        LoginSession session = sessions.findById(claims.sessionId())
                .filter(s -> s.isActive(Instant.now()))
                .filter(s -> s.getUser().getId().equals(claims.userId()))
                .orElseThrow(() -> BusinessException.unauthorized("Session is no longer valid"));
        return new Authenticated(session.getUser(), session.getId());
    }

    private String randomToken() {
        byte[] buffer = new byte[32];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(value)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
