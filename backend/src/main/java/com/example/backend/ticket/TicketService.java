package com.example.backend.ticket;

import com.example.backend.user.User;
import com.example.backend.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<TicketView> listTickets(User actor) {
        List<Ticket> tickets = actor.getRole() == Role.USER
                ? ticketRepository.findByCreatedById(actor.getId())
                : ticketRepository.findAll();
        return tickets.stream()
                .map(TicketView::of)
                .toList();
    }
}
