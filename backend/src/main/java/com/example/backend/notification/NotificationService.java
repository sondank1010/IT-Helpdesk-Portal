package com.example.helpdesk.notification;

import com.example.helpdesk.auth.CurrentUser;
import com.example.helpdesk.common.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }
    public record NotificationView(Long id, Long ticketId, String message, boolean read, Instant createdAt) {
        static NotificationView of(Notification n) {
            return new NotificationView(n.getId(), n.getTicketId(), n.getMessage(), n.isReadFlag(), n.getCreatedAt());
        }
    }
    @Transactional(readOnly = true)
    public List<NotificationView> list() {
        return notifications.findByUserIdOrderByCreatedAtDesc(CurrentUser.get().getId()).stream()
                .map(NotificationView::of).toList();
    }
    @Transactional
    public NotificationView read(Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(CurrentUser.get().getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Notification belongs to another user");
        n.setReadFlag(true); return NotificationView.of(n);
    }
}
