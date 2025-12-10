package com.imp3.Backend.notification;

import com.imp3.Backend.common.AbstractController;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notif")
public class NotificationController extends AbstractController {

    @Autowired
    NotificationRepository notificationrepository;

    @Autowired
    UserRepository userrepository;

    @Autowired
    NotificationService notificationservice;

    /**
     * CREATE (POST)
     * Creates a new notification when an event occurs
     * @param body (JSON) of NotificationRequest type
     * @param session HttpSession containing "uid"
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notification createNotification(@RequestBody NotificationRequest body, HttpSession session){
        //Validate session user (uid)
        Integer currentId = getSessionUid(session);

        Integer recipientId = body.getRecipientId();
        String message = body.getMessage();
        String type = body.getType();

        //validate recipient id, cannot be null nor = currentId
        if(recipientId == null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient ID cannot be null.");
        if(recipientId.equals(currentId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient ID cannot be the same as the current ID.");

        //validate message (cannot be null, or above 255 characters)
        if(message == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be null or empty.");
        if(message.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot exceed 255 characters.");

        //fetch users and validate they exist
        User current = userrepository.findById(currentId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found."));
        User recipient = userrepository.findById(recipientId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found."));

        //create new notification
        Notification n = new Notification();
        n.setActor(current);
        n.setRecipient(recipient);
        n.setType(type != null ? type: "GENERIC");
        n.setMessage(message.trim());
        n.setCreatedAt(java.time.LocalDateTime.now());
        n.setReadAt(null);

        //save to DB
        Notification saved = notificationrepository.save(n);

        //push WS
        notificationservice.push(saved);

        return saved;
    }

    /**
     * READ (GET)
     * Fetch one specific notification of the user
     * @param notifId of notification
     * @param session HttpSession containing "uid"
     * @return the requested notification
     */
    @GetMapping("/{notifId}")
    public Notification getNotification(@PathVariable Integer notifId, HttpSession session){
        //Validate session user (uid)
       Integer currentId = getSessionUid(session);

       return notificationrepository.findByNotifIdAndRecipient_Id(notifId, currentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * LIST (GET)
     * @param session HttpSession containing "uid"
     * @return list of all notifications
     */
    @GetMapping
    public List<Notification> getAllNotifications(HttpSession session){
        //Validate session user (uid)
        Integer currentId = getSessionUid(session);

        return notificationrepository.findByRecipient_IdOrderByCreatedAtDesc(currentId);
    }


    /**
     * UPDATE (PUT)
     * Marks a notification as read
     * @param notifId of notification
     * @param session HttpSession containing "uid"
     * @return the updated notification (now read)
     */
    @PutMapping("/{notifId}/read")
    public Notification markRead(@PathVariable Integer notifId, HttpSession session){
        //Validate session user (uid)
        Integer currentId = getSessionUid(session);

        Notification n = notificationrepository.findByNotifIdAndRecipient_Id(notifId, currentId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found."));
        //mark as read
        n.setReadAt(java.time.LocalDateTime.now());

        //save updated notification
        return notificationrepository.save(n);
    }

    /**
     * UPDATE (PUT) -- Marks all notifications as read
     * @param session HttpSession containing "uid"
     * @return list of all notifications (that are now marked as read)
     */
    @PutMapping("/read")
    public List<Notification> markAllAsRead(HttpSession session){
        //Validate session user (uid)
        Integer currentId = getSessionUid(session);

        List<Notification> notifs = notificationrepository.findByRecipient_IdOrderByCreatedAtDesc(currentId);

        LocalDateTime now = LocalDateTime.now();
        boolean nowRead = false;
        for(Notification n : notifs){
            if(n.getReadAt() == null){
                n.setReadAt(now);
                nowRead = true;
            }
        }
        return nowRead ? notificationrepository.saveAll(notifs) : notifs;
    }

    /**
     * DELETE (DELETE)
     * Removes a notification from a user's list
     * @param notifId of notification
     * @param session HttpSession containing "uid"
     */
    @DeleteMapping("/{notifId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable Integer notifId, HttpSession session){
        //Validate session user (uid)
        Integer currentId = getSessionUid(session);

        //confirms the notification exists and belongs to the user
        Notification notif = notificationrepository.findByNotifIdAndRecipient_Id(notifId, currentId).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        notificationrepository.delete(notif);
    }

}
