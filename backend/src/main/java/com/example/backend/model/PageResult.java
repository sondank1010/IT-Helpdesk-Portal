package com.example.backend.model;

import java.util.List;
import java.util.function.Function;

/** Kết quả phân trang độc lập với Spring Data, để tầng nghiệp vụ không phụ thuộc thư viện DB. */
public record PageResult<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), totalElements, totalPages, number, size);
    }
}
