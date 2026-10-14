package com.example.backend.ticket;

public record HistoryView(
        Long id, Long actorId, String action, String details, java.time.Instant createdAt
) {
    static HistoryView of(TicketHistory h) {
        return new HistoryView(h.getId(), h.getActor().getId(), h.getAction(), h.getDetails(), h.getCreatedAt());
    }
}
