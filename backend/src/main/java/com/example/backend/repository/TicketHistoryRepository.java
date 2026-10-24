package com.example.backend.repository;

import com.example.backend.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
