package com.example.backend.security;

import com.example.helpdesk.service.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** check access token JWT ký HS256, valid 15' */
@Component
public class JwtService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final ObjectMapper mapper;

    public record Claims(Long userId, String sessionId) { }

    public JwtService(@Value("${app.jwt-secret}") String secret, ObjectMapper mapper) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        this.secret = bytes;
        this.mapper = mapper;
    }

    public String issue(Long userId, String sessionId) {
        try {
            String header = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(mapper.writeValueAsBytes(Map.of(
                    "sub", userId.toString(),
                    "sid", sessionId,
                    "exp", Instant.now().plus(ACCESS_TOKEN_TTL).getEpochSecond())));
            String unsigned = header + "." + payload;
            return unsigned + "." + encode(sign(unsigned));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot issue token", ex);
        }
    }

    public Claims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), DECODER.decode(parts[2]))) {
                throw invalidToken();
            }
            JsonNode header = mapper.readTree(DECODER.decode(parts[0]));
            JsonNode body = mapper.readTree(DECODER.decode(parts[1]));
            boolean expired = body.path("exp").asLong() <= Instant.now().getEpochSecond();
            if (!"HS256".equals(header.path("alg").asText()) || expired) {
                throw invalidToken();
            }
            return new Claims(Long.parseLong(body.path("sub").asText()), body.path("sid").asText());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidToken();
        }
    }

    private byte[] sign(String text) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    private static BusinessException invalidToken() {
        return BusinessException.unauthorized("Invalid or expired token");
    }
}
