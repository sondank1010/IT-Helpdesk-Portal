package com.example.backend.ticket;

import com.example.backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ticket_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false)
    private User actor;
    @Column(nullable = false)
    private String action;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
