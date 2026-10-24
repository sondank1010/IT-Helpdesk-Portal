package com.example.backend.service;


import com.example.backend.model.*;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.TicketHistoryRepository;
import com.example.backend.repository.TicketRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.hibernate.dialect.SybaseASEDialect.MAX_PAGE_SIZE;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categories;
    private final TicketHistoryRepository history;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, CategoryRepository categories, TicketHistoryRepository history, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categories = categories;
        this.history = history;
        this.userRepository = userRepository;
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

    public record HistoryView(Long id, Long actorId, String action, String details, Instant createdAt) {
        static HistoryView of(TicketHistory h) {
            return new HistoryView(h.getId(), h.getActor().getId(), h.getAction(), h.getDetails(), h.getCreatedAt());
        }
    }

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

    private static boolean isAllowedTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case NEW -> to == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TicketStatus.RESOLVED;
            case RESOLVED -> to == TicketStatus.CLOSED || to == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }


    @Transactional
    public TicketView changeStatus(Long id, TicketStatus target, User actor) {
        Ticket ticket = findVisible(id, actor);
        TicketStatus current = ticket.getStatus();
        if (actor.getRole() == Role.USER && !(current == TicketStatus.RESOLVED && target == TicketStatus.CLOSED)) {
            throw BusinessException.forbidden("User can only close a RESOLVED ticket");
        }
        if (actor.getRole() == Role.AGENT && !ticket.isAssignedTo(actor)) {
            throw BusinessException.forbidden("Only the assigned agent can change status");
        }
        if (!isAllowedTransition(current, target)) {
            throw BusinessException.invalid("Invalid status transition " + current + " -> " + target);
        }
        ticket.setStatus(target);
        ticket.touch();
        record(ticket, actor, "STATUS_CHANGED", current + " -> " + target);
        return TicketView.of(ticket);
    }

    private static void requireAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw BusinessException.forbidden("Admin only");
        }
    }

    @Transactional
    public TicketView assign(Long id, Long agentId, User actor) {
        requireAdmin(actor);
        Ticket ticket = findTicket(id);
        User agent = userRepository.findById(agentId).orElseThrow(() -> BusinessException.notFound("Agent not found"));
        if (agent.getRole() != Role.AGENT) {
            throw BusinessException.invalid("Target user is not an agent");
        }
        ticket.setAssignedTo(agent);
        ticket.touch();
        record(ticket, actor, "ASSIGNED", "Assigned to user #" + agentId);
        return TicketView.of(ticket);
    }

    /** Xóa hẳn ticket và lịch sử. USER chỉ xóa ticket NEW của mình; AGENT không được xóa. */
    @Transactional
    public void delete(Long id, User actor) {
        Ticket ticket = findVisible(id, actor);
        if (actor.getRole() == Role.AGENT) {
            throw BusinessException.forbidden("Agent cannot delete tickets");
        }
        if (actor.getRole() == Role.USER && ticket.getStatus() != TicketStatus.NEW) {
            throw BusinessException.forbidden("User can delete only NEW tickets");
        }
        history.deleteByTicketId(id);
        ticketRepository.delete(ticket);
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(Long id, User actor) {
        findVisible(id, actor);
        return history.findByTicketIdOrderByCreatedAtAscIdAsc(id).stream().map(HistoryView::of).toList();
    }
}
