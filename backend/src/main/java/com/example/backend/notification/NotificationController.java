package com.example.helpdesk.notification;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping
    public List<NotificationService.NotificationView> list() { return service.list(); }
    @PatchMapping("/{id}/read")
    public NotificationService.NotificationView read(@PathVariable Long id) { return service.read(id); }
}
