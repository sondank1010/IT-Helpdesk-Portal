package com.example.backend.ticket;

import com.example.backend.category.Category;
import com.example.backend.category.CategoryRepository;
import com.example.backend.ticket.enums.TicketStatus;
import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
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
    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;

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

        Ticket savedTicket = ticketRepository.save(ticket);
        log(savedTicket, actor, "CREATED", "Ticket created");
        return TicketView.of(savedTicket);
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
        log(ticket, actor, "UPDATED", "Ticket information updated");
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
                case RESOLVED -> true;
                case CLOSED -> false;
            };
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
            }
        }
        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now());
        log(ticket, actor, "STATUS_CHANGED", "Status changed from " + current + " to " + status);
        return TicketView.of(ticket);
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(Long id,User actor) {
        visibleTicket(actor, id);
        return ticketHistoryRepository.findByTicketIdOrderByCreatedAtAsc(id).stream().map(HistoryView::of).toList();
    }

    @Transactional
    public void deleteTicket(Long id, User actor) {
        Ticket ticket = visibleTicket(actor, id);
        if (actor.getRole() != Role.ADMIN && ticket.getStatus() != TicketStatus.NEW)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User can delete only NEW tickets");
        if (actor.getRole() == Role.AGENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agent cannot delete tickets");
        if(actor.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(actor.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User can only delete tickets they created.");
        ticketHistoryRepository.deleteByTicketId(id);
        ticketRepository.delete(ticket);
    }

    @Transactional
    public TicketView assignTicket(User actor, Long agentId, Long ticketId) {
        User agent  = userRepository.findById(agentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (actor.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        if (agent.getRole() != Role.AGENT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not an agent");
        ticket.setAssignedTo(agent);
        ticket.setUpdatedAt(Instant.now());
        log(ticket, actor, "ASSIGNED", "Assigned to user #" + agentId);
        return TicketView.of(ticket);
    }


    private void log(Ticket ticket, User actor, String action, String details) {
        TicketHistory item = new TicketHistory(); item.setTicket(ticket); item.setActor(actor);
        item.setAction(action); item.setDetails(details); ticketHistoryRepository.save(item);
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
