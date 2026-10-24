package com.example.backend.api;

import com.example.backend.model.PageResult;
import com.example.backend.model.Priority;
import com.example.backend.model.TicketStatus;
import com.example.backend.service.TicketService;
import com.example.backend.service.TicketService.TicketView;
import com.example.backend.service.TicketService.TicketFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @RequestMapping(path = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public TicketView update(@PathVariable Long id, @RequestBody UpdateTicket r) {
        return tickets.update(id, r.title(), r.description(), r.priority(), r.categoryId(), CurrentUser.get());
    }


    @PatchMapping("/{id}/status")
    public TicketView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange r) {
        return tickets.changeStatus(id, r.status(), CurrentUser.get());
    }


    @PatchMapping("/{id}/assign")
    public TicketView assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r) {
        return tickets.assign(id, r.agentId(), CurrentUser.get());
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tickets.delete(id, CurrentUser.get());
    }


    @GetMapping("/{id}/history")
    public List<TicketService.HistoryView> history(@PathVariable Long id) {
        return tickets.history(id, CurrentUser.get());
    }

}
