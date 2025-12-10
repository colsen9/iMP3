package com.imp3.Backend.notification;
import com.imp3.Backend.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    //LIST -- all notifications for a user, newest first
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Integer recipientId);

    //READ - get one notification, and make sure it belongs to the current user
    Optional<Notification> findByNotifIdAndRecipient_Id(Integer NotifId, Integer recipientId);

    //DELETE - Remove a notification owned by this user
    void deleteByNotifIdAndRecipient_Id(Integer NotifId, Integer recipientId);

    //COUNT -- quick unread count for badges
    long countByRecipient_IdAndReadAtIsNull(Integer recipientId);
}
