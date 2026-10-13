package com.example.backend.ticket;

import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketControllerTest {
    private TicketService ticketService;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ticketService = mock(TicketService.class);
        userRepository = mock(UserRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TicketController(ticketService, userRepository)).build();
    }

    @Test
    void listsTicketsUsingUserLoadedFromDatabase() throws Exception {
        User actor = new User();
        actor.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(ticketService.listTickets(actor)).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(ticketService).listTickets(actor);
    }

    @Test
    void rejectsMissingUserHeader() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, ticketService);
    }

    @Test
    void rejectsNonNumericUserHeader() throws Exception {
        mockMvc.perform(get("/api/tickets").header("X-User-Id", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, ticketService);
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tickets").header("X-User-Id", "99"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(ticketService);
    }
    @Test
    void patchAcceptsPartialUpdateButPutRequiresAllFields() throws Exception {
        User actor = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        mockMvc.perform(patch("/api/tickets/10").header("X-User-Id", "1")
                        .contentType("application/json").content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
        verify(ticketService).updateTicket(actor, 10L, new TicketUpdateRequest("Updated", null, null, null));
        clearInvocations(ticketService);
        mockMvc.perform(put("/api/tickets/10").header("X-User-Id", "1")
                        .contentType("application/json").content("{\"title\":\"Updated\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ticketService);
    }

    @Test
    void statusEndpointValidatesAndDelegates() throws Exception {
        User actor = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        mockMvc.perform(patch("/api/tickets/10/status").header("X-User-Id", "1")
                        .contentType("application/json").content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());
        verify(ticketService).updateStatus(actor, 10L, com.example.backend.ticket.enums.TicketStatus.RESOLVED);
        clearInvocations(ticketService);
        for (String body : List.of("{}", "{\"status\":\"INVALID\"}")) {
            mockMvc.perform(patch("/api/tickets/10/status").header("X-User-Id", "1")
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(ticketService);
    }
}
