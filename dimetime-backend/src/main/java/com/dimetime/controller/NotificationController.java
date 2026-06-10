package com.dimetime.controller;

import com.dimetime.entity.Notification;
import com.dimetime.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam("username") String username,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(username, unreadOnly));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            Notification result = notificationService.markAsRead(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam("username") String username) {
        return ResponseEntity.ok(notificationService.getUnreadCount(username));
    }
}
