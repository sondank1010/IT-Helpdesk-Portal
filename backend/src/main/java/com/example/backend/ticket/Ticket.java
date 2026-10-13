package com.example.backend.ticket;

import com.example.backend.category.Category;
import com.example.backend.ticket.enums.TicketStatus;
import com.example.backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import com.example.backend.ticket.enums.Priority;
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketStatus status = TicketStatus.NEW;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedTo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private Category category;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
