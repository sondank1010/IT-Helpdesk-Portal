package com.example.backend.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketAssignRequest(@NotNull @Positive Long agentId) {}
