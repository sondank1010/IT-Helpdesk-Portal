package com.example.backend.ticket;

import com.example.backend.ticket.enums.Priority;
import com.example.backend.ticket.enums.TicketStatus;

import java.time.Instant;

public record TicketView(
        Long id,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        Long categoryId,
        String categoryName,
        Long createdById,
        Long assignedToId,
        Instant createdAt,
        Instant updatedAt) {

    static TicketView of(Ticket t) {
        return new TicketView(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getStatus(),
                t.getCategory().getId(), t.getCategory().getName(), t.getCreatedBy().getId(),
                t.getAssignedTo() == null ? null : t.getAssignedTo().getId(), t.getCreatedAt(), t.getUpdatedAt());
    }
}