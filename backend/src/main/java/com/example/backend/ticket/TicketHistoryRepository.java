package com.example.backend.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    List<TicketHistory> findByTicketIdOrderByCreatedAtAsc(Long id);

    void deleteByTicketId(Long ticketId);
}
