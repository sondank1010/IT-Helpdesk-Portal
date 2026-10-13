package com.example.backend.ticket;

import com.example.backend.ticket.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TicketCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotNull @Positive Long categoryId,
        Priority priority) {
}
