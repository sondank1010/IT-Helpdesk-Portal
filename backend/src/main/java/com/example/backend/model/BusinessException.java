package com.example.backend.model;

/**
 * Lỗi nghiệp vụ. Tầng API ánh xạ {@link Kind} sang mã HTTP, nên tầng nghiệp vụ không cần biết về HTTP.
 */
public class BusinessException extends RuntimeException {

    public enum Kind { INVALID, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT }

    private final Kind kind;

    public BusinessException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    public static BusinessException invalid(String message) { return new BusinessException(Kind.INVALID, message); }
    public static BusinessException unauthorized(String message) { return new BusinessException(Kind.UNAUTHORIZED, message); }
    public static BusinessException forbidden(String message) { return new BusinessException(Kind.FORBIDDEN, message); }
    public static BusinessException notFound(String message) { return new BusinessException(Kind.NOT_FOUND, message); }
    public static BusinessException conflict(String message) { return new BusinessException(Kind.CONFLICT, message); }
}
