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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/tickets").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(ticketService).listTickets(actor);
    }

    @Test
    void rejectsMissingUserHeader() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, ticketService);
    }

    @Test
    void rejectsNonNumericUserHeader() throws Exception {
        mockMvc.perform(get("/tickets").header("X-User-Id", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userRepository, ticketService);
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tickets").header("X-User-Id", "99"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(ticketService);
    }
}
