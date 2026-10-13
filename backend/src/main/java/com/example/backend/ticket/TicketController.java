package com.example.backend.ticket;

import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final UserRepository userRepository;


    @GetMapping
    public ResponseEntity<List<TicketView>> findAll(@RequestHeader("X-User-Id") Long userId) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(ticketService.listTickets(actor));
    }

    @PostMapping
    public ResponseEntity<TicketView> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TicketCreateRequest request) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(actor, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketView> findById(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(ticketService.getTicketById(actor, id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<TicketView> update(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
            @Validated(TicketUpdateRequest.FullUpdate.class) @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(actor(userId), id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TicketView> patch(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
            @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(actor(userId), id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketView> status(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
            @Valid @RequestBody TicketStatusRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(actor(userId), id, request.status()));
    }

    private User actor(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }


}
