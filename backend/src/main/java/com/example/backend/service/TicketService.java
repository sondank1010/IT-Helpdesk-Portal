package com.example.backend.service;


import com.example.backend.model.*;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.TicketHistoryRepository;
import com.example.backend.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

    private Category findCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> BusinessException.notFound("Category not found"));
    }

    private void record(Ticket ticket, User actor, String action, String details) {
        history.save(new TicketHistory(ticket, actor, action, details));
    }
}
