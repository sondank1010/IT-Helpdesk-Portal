package com.example.backend.repository;

import com.example.backend.model.PageResult;
import com.example.backend.model.Priority;
import com.example.backend.model.Ticket;
import com.example.backend.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            select t from Ticket t where
            (:ownerId is null or t.createdBy.id = :ownerId) and
            (:status is null or t.status = :status) and
            (:priority is null or t.priority = :priority) and
            (:categoryId is null or t.category.id = :categoryId) and
            (:agentId is null or t.assignedTo.id = :agentId)
            """)
    Page<Ticket> searchPage(@Param("ownerId") Long ownerId, @Param("status") TicketStatus status,
                            @Param("priority") Priority priority, @Param("categoryId") Long categoryId,
                            @Param("agentId") Long agentId, Pageable pageable);

    /** Bọc kết quả Spring Data thành PageResult, ticket mới nhất đứng trước. */
    default PageResult<Ticket> search(Long ownerId, TicketStatus status, Priority priority, Long categoryId,
                                      Long agentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> result = searchPage(ownerId, status, priority, categoryId, agentId, pageable);
        return new PageResult<>(result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize());
    }

    long countByCategoryId(Long categoryId);
}
