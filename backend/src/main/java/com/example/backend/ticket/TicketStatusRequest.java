package com.example.backend.ticket;

import com.example.backend.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusRequest(@NotNull TicketStatus status) {}
