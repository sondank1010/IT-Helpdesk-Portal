package com.example.backend.ticket;

import com.example.backend.ticket.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(
        @NotBlank(groups = FullUpdate.class) @Pattern(regexp = "(?s).*\\S.*") @Size(max = 200) String title,
        @NotBlank(groups = FullUpdate.class) @Pattern(regexp = "(?s).*\\S.*") String description,
        @NotNull(groups = FullUpdate.class) @Positive Long categoryId,
        @NotNull(groups = FullUpdate.class) Priority priority) {
    public interface FullUpdate extends jakarta.validation.groups.Default {}
}
