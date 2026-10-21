package com.example.backend.api;

import com.example.backend.model.Priority;
import com.example.backend.model.Ticket;
import com.example.backend.model.TicketStatus;
import com.example.backend.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Ticket")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    public record CreateTicket(@NotBlank @Size(max = 200) String title, @NotBlank String description,
                               Priority priority, @NotNull Long categoryId) { }

    public record UpdateTicket(String title, String description, Priority priority, Long categoryId) { }

    public record StatusChange(@NotNull TicketStatus status) { }

    public record AssignRequest(@NotNull Long agentId) { }

    public record TicketView(Long id, String title, String description, Priority priority, TicketStatus status,
                             Long categoryId, String categoryName, Long createdById, Long assignedToId,
                             Instant createdAt, Instant updatedAt) {
        static TicketView of(Ticket t) {
            return new TicketView(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getStatus(),
                    t.getCategory().getId(), t.getCategory().getName(), t.getCreatedBy().getId(),
                    t.getAssignedTo() == null ? null : t.getAssignedTo().getId(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView create(@Valid @RequestBody CreateTicket r) {
        return tickets.create(r.title(), r.description(), r.priority(), r.categoryId(), CurrentUser.get());
    }

}
