package com.example.backend.api;

import com.example.backend.model.PageResult;
import com.example.backend.model.Priority;
import com.example.backend.model.TicketStatus;
import com.example.backend.service.TicketService;
import com.example.backend.service.TicketService.TicketView;
import com.example.backend.service.TicketService.TicketFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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



    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView create(@Valid @RequestBody CreateTicket r) {
        return tickets.create(r.title(), r.description(), r.priority(), r.categoryId(), CurrentUser.get());
    }

    @GetMapping
    public PageResult<TicketView> list(@RequestParam(required = false) TicketStatus status,
                                       @RequestParam(required = false) Priority priority,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Long agentId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        TicketFilter filter = new TicketFilter(status, priority, categoryId, agentId);
        return tickets.list(filter, page, size, CurrentUser.get());
    }

    @GetMapping("/{id}")
    public TicketView get(@PathVariable Long id) {
        return tickets.get(id, CurrentUser.get());
    }

}
