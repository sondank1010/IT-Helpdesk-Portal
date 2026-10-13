package com.example.backend.ticket;

import com.example.backend.category.Category;
import com.example.backend.category.CategoryRepository;
import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketCreateTest {
    private static final String VALID_BODY = """
            {"title":"Printer issue","description":"Cannot print","categoryId":2}
            """;

    private TicketRepository ticketRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        userRepository = mock(UserRepository.class);
        TicketService service = new TicketService(ticketRepository, categoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(service, userRepository)).build();

        User actor = new User();
        actor.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        Category category = new Category();
        category.setId(2L);
        category.setName("Hardware");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(10L);
            return ticket;
        });
    }

    @Test
    void createsTicketWithServerDefaultsAndCreatorFromHeader() throws Exception {
        mockMvc.perform(post("/tickets").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Printer issue"))
                .andExpect(jsonPath("$.description").value("Cannot print"))
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.categoryName").value("Hardware"))
                .andExpect(jsonPath("$.createdById").value(1))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.assignedToId").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void acceptsExplicitPriority() throws Exception {
        String body = """
                {"title":"Printer issue","description":"Cannot print","categoryId":2,"priority":"HIGH"}
                """;
        mockMvc.perform(post("/tickets").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"title\":\" \",\"description\":\"Issue\",\"categoryId\":2}",
            "{\"title\":\"Issue\",\"description\":\" \",\"categoryId\":2}",
            "{\"title\":\"Issue\",\"description\":\"Details\",\"categoryId\":0}",
            "{\"title\":\"Issue\",\"description\":\"Details\"}",
            "{\"title\":\"Issue\",\"description\":\"Details\",\"categoryId\":2,\"priority\":\"INVALID\"}"
    })
    void rejectsInvalidBodyWithoutSaving(String body) throws Exception {
        mockMvc.perform(post("/tickets").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void rejectsTitleLongerThanDatabaseLimit() throws Exception {
        String body = "{\"title\":\"" + "x".repeat(201)
                + "\",\"description\":\"Details\",\"categoryId\":2}";
        mockMvc.perform(post("/tickets").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void rejectsMissingUserHeader() throws Exception {
        mockMvc.perform(post("/tickets").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void rejectsUnknownUserWithoutSaving() throws Exception {
        mockMvc.perform(post("/tickets").header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void rejectsUnknownCategoryWithoutSaving() throws Exception {
        when(categoryRepository.findById(2L)).thenReturn(Optional.empty());
        mockMvc.perform(post("/tickets").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ticketRepository);
    }
}
