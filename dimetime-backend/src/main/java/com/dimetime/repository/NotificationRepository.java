package com.dimetime.repository;

import com.dimetime.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
    List<Notification> findByUsernameAndIsReadOrderByCreatedAtDesc(String username, Boolean isRead);
    long countByUsernameAndIsRead(String username, Boolean isRead);
}
