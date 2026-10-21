package com.example.backend.service;


import com.example.backend.model.*;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.TicketHistoryRepository;
import com.example.backend.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hibernate.dialect.SybaseASEDialect.MAX_PAGE_SIZE;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categories;
    private final TicketHistoryRepository history;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categories, TicketHistoryRepository history) {
        this.ticketRepository = ticketRepository;
        this.categories = categories;
        this.history = history;
    }

    public record TicketView(Long id, String title, String description, Priority priority, TicketStatus status,
                             Long categoryId, String categoryName, Long createdById, Long assignedToId,
                             Instant createdAt, Instant updatedAt) {
        static TicketView of(Ticket t) {
            return new TicketView(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getStatus(),
                    t.getCategory().getId(), t.getCategory().getName(), t.getCreatedBy().getId(),
                    t.getAssignedTo() == null ? null : t.getAssignedTo().getId(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record TicketFilter(TicketStatus status, Priority priority, Long categoryId, Long agentId) { }


    @Transactional
    public TicketView create(String title, String description, Priority priority, Long categoryId, User actor) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(priority == null ? Priority.MEDIUM : priority);
        ticket.setCategory(findCategory(categoryId));
        ticket.setCreatedBy(actor);
        ticketRepository.save(ticket);
        record(ticket, actor, "CREATED", "Ticket created");
        return TicketView.of(ticket);
    }

    @Transactional(readOnly = true)
    public PageResult<TicketView> list(TicketFilter filter, int page, int size, User actor) {
        Long ownerId = actor.getRole() == Role.USER ? actor.getId() : null;
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return ticketRepository.search(ownerId, filter.status(), filter.priority(), filter.categoryId(), filter.agentId(),
                safePage, safeSize).map(TicketView::of);
    }

    private Category findCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> BusinessException.notFound("Category not found"));
    }

    private void record(Ticket ticket, User actor, String action, String details) {
        history.save(new TicketHistory(ticket, actor, action, details));
    }

    @Transactional(readOnly = true)
    public TicketView get(Long id, User actor) {
        return TicketView.of(findVisible(id, actor));
    }

    @Transactional
    public TicketView update(Long id, String title, String description, Priority priority, Long categoryId,
                             User actor) {
        Ticket ticket = findVisible(id, actor);
        if (actor.getRole() == Role.USER && ticket.getStatus() != TicketStatus.NEW) {
            throw BusinessException.forbidden("User can edit only NEW tickets");
        }
        if (actor.getRole() == Role.AGENT && !ticket.isAssignedTo(actor)) {
            throw BusinessException.forbidden("Only the assigned agent can edit this ticket");
        }
        if (title != null) {
            if (title.isBlank() || title.length() > 200) {
                throw BusinessException.invalid("title must be 1-200 characters");
            }
            ticket.setTitle(title.trim());
        }
        if (description != null) {
            if (description.isBlank()) {
                throw BusinessException.invalid("description must not be blank");
            }
            ticket.setDescription(description.trim());
        }
        if (priority != null) {
            ticket.setPriority(priority);
        }
        if (categoryId != null) {
            ticket.setCategory(findCategory(categoryId));
        }
        ticket.touch();
        record(ticket, actor, "UPDATED", "Ticket details updated");
        return TicketView.of(ticket);
    }

    private Ticket findVisible(Long id, User actor) {
        Ticket ticket = findTicket(id);
        if (actor.getRole() == Role.USER && !ticket.isOwnedBy(actor)) {
            throw BusinessException.forbidden("Ticket belongs to another user");
        }
        return ticket;
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Ticket not found"));
    }
}
