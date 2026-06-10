package com.dimetime.service;

import com.dimetime.entity.Notification;
import com.dimetime.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> getNotificationsForUser(String username, Boolean unreadOnly) {
        if (unreadOnly != null && unreadOnly) {
            return notificationRepository.findByUsernameAndIsReadOrderByCreatedAtDesc(username, false);
        }
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Optional<Notification> notifOpt = notificationRepository.findById(id);
        if (notifOpt.isEmpty()) {
            throw new IllegalArgumentException("Notification not found: " + id);
        }
        Notification notification = notifOpt.get();
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void createNotification(String username, String message) {
        Notification notification = new Notification(username, message, false);
        notificationRepository.save(notification);
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByUsernameAndIsRead(username, false);
    }
}
