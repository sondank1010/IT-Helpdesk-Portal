package com.example.backend.ticket;

import com.example.backend.category.Category;
import com.example.backend.category.CategoryRepository;
import com.example.backend.ticket.enums.TicketStatus;
import com.example.backend.user.User;
import com.example.backend.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {
    private TicketRepository ticketRepository;
    private TicketService service;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        service = new TicketService(ticketRepository, mock(CategoryRepository.class));
        Category category = new Category();
        category.setId(2L);
        category.setName("Hardware");
        ticket = new Ticket();
        ticket.setId(10L);
        ticket.setTitle("Printer issue");
        ticket.setDescription("Cannot print");
        ticket.setCategory(category);
        ticket.setCreatedBy(user(1000L, Role.USER));
        ticket.setAssignedTo(user(2000L, Role.AGENT));
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
    }

    @Test
    void creatorCanViewTicketUsingMatchingId() {
        TicketView view = service.getTicketById(user(1000L, Role.USER), 10L);

        assertEquals(10L, view.id());
        assertEquals("Printer issue", view.title());
        assertEquals(1000L, view.createdById());
    }

    @Test
    void assigneeCanViewTicketUsingMatchingId() {
        TicketView view = service.getTicketById(user(2000L, Role.AGENT), 10L);

        assertEquals(10L, view.id());
        assertEquals(2000L, view.assignedToId());
    }

    @Test
    void unrelatedAdminCanViewTicket() {
        assertEquals(10L, service.getTicketById(user(3000L, Role.ADMIN), 10L).id());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"USER", "AGENT"})
    void unrelatedUserOrAgentCannotViewTicket(Role role) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getTicketById(user(3000L, role), 10L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void creatorCanViewUnassignedTicket() {
        ticket.setAssignedTo(null);

        assertNull(service.getTicketById(user(1000L, Role.USER), 10L).assignedToId());
    }

    @Test
    void unrelatedAgentCannotViewUnassignedTicket() {
        ticket.setAssignedTo(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getTicketById(user(3000L, Role.AGENT), 10L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void missingTicketReturnsNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getTicketById(user(1000L, Role.USER), 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void statusPermissionsCoverEveryRoleAndTransition() {
        for (Role role : Role.values()) {
            User actor = user(role == Role.USER ? 1000L : 2000L, role);
            for (TicketStatus from : TicketStatus.values()) {
                for (TicketStatus to : TicketStatus.values()) {
                    ticket.setStatus(from);
                    boolean allowed = role == Role.ADMIN
                            || (role == Role.USER && to == TicketStatus.CLOSED
                                && (from == TicketStatus.RESOLVED || from == TicketStatus.CLOSED))
                            || (role == Role.AGENT && (
                                (to == TicketStatus.IN_PROGRESS && from != TicketStatus.CLOSED)
                                || (to == TicketStatus.RESOLVED
                                    && (from == TicketStatus.IN_PROGRESS || from == TicketStatus.RESOLVED))));
                    if (allowed) {
                        assertEquals(to, service.updateStatus(actor, 10L, to).status());
                    } else {
                        assertThrows(ResponseStatusException.class, () -> service.updateStatus(actor, 10L, to));
                        assertEquals(from, ticket.getStatus());
                    }
                }
            }
        }
    }

    @Test
    void agentWhoCreatedTicketStillNeedsAssignmentToChangeStatus() {
        ticket.setCreatedBy(user(3000L, Role.AGENT));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(user(3000L, Role.AGENT), 10L, TicketStatus.IN_PROGRESS));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void repeatedStatusDoesNotChangeTimestamp() {
        ticket.setStatus(TicketStatus.RESOLVED);
        var updatedAt = ticket.getUpdatedAt();
        service.updateStatus(user(2000L, Role.AGENT), 10L, TicketStatus.RESOLVED);
        assertEquals(updatedAt, ticket.getUpdatedAt());
    }

    @Test
    void partialUpdatePreservesOtherFields() {
        service.updateTicket(user(1000L, Role.USER), 10L,
                new TicketUpdateRequest("Updated title", null, null, null));
        assertEquals("Updated title", ticket.getTitle());
        assertEquals("Cannot print", ticket.getDescription());
        assertEquals(2L, ticket.getCategory().getId());
        assertEquals(TicketStatus.NEW, ticket.getStatus());
    }

    @Test
    void unrelatedUserCannotUpdateTicket() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateTicket(user(3000L, Role.USER), 10L,
                        new TicketUpdateRequest("Changed", null, null, null)));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Printer issue", ticket.getTitle());
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
