package com.example.backend.ticket;

import com.example.backend.category.Category;
import com.example.backend.category.CategoryRepository;
import com.example.backend.ticket.enums.TicketStatus;
import com.example.backend.user.User;
import com.example.backend.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<TicketView> listTickets(User actor) {
        List<Ticket> tickets = actor.getRole() == Role.USER
                ? ticketRepository.findByCreatedById(actor.getId())
                : ticketRepository.findAll();
        return tickets.stream()
                .map(TicketView::of)
                .toList();
    }

    @Transactional
    public TicketView createTicket(User actor, TicketCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(category);
        ticket.setCreatedBy(actor);
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }

        return TicketView.of(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketView getTicketById(User actor, Long ticketId) {
        return TicketView.of(visibleTicket(actor, ticketId));
    }

    @Transactional
    public TicketView updateTicket(User actor, Long ticketId, TicketUpdateRequest request) {
        Ticket ticket = visibleTicket(actor, ticketId);
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            ticket.setCategory(category);
        }
        if (request.title() != null) ticket.setTitle(request.title());
        if (request.description() != null) ticket.setDescription(request.description());
        if (request.priority() != null) ticket.setPriority(request.priority());
        ticket.setUpdatedAt(Instant.now());
        return TicketView.of(ticket);
    }

    @Transactional
    public TicketView updateStatus(User actor, Long ticketId, TicketStatus status) {
        Ticket ticket = visibleTicket(actor, ticketId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        TicketStatus current = ticket.getStatus();
        if (actor.getRole() == Role.USER) {
            if (!actor.getId().equals(ticket.getCreatedBy().getId()) || status != TicketStatus.CLOSED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users can only close their own resolved tickets");
            }
        } else if (actor.getRole() == Role.AGENT) {
            if (ticket.getAssignedTo() == null || !actor.getId().equals(ticket.getAssignedTo().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned agent can change status");
            }
            if (status != TicketStatus.IN_PROGRESS && status != TicketStatus.RESOLVED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agents can only start, reopen or resolve tickets");
            }
        } else if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change ticket status");
        }
        if (current == status) return TicketView.of(ticket);
        if (actor.getRole() != Role.ADMIN) {
            boolean allowed = switch (current) {
                case NEW -> status == TicketStatus.IN_PROGRESS;
                case IN_PROGRESS -> status == TicketStatus.RESOLVED;
                case RESOLVED -> status == TicketStatus.CLOSED || status == TicketStatus.IN_PROGRESS;
                case CLOSED -> false;
            };
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
            }
        }
        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now());
        return TicketView.of(ticket);
    }

    private Ticket visibleTicket(User actor, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        boolean canView = actor.getRole() == Role.ADMIN
                || actor.getId().equals(ticket.getCreatedBy().getId())
                || (ticket.getAssignedTo() != null
                    && actor.getId().equals(ticket.getAssignedTo().getId()));
        if (!canView) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this ticket");
        }

        return ticket;
    }
}
