package com.example.backend.repository;

import com.example.backend.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
