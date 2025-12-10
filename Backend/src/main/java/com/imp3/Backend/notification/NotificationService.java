package com.imp3.Backend.notification;

import com.imp3.Backend.follow.FollowRepository;
import com.imp3.Backend.list.UserList;
import com.imp3.Backend.reviews.Review;
import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final FollowRepository followrepository;
    private final NotificationRepository notificationrepository;
    private final UserRepository userrepository;

    /**
     * Notifies the user when a follower posts a new review
     * @param review that was just posted by follower
     */
    @Transactional
    public void notifyFollowersOfReview(Review review){
        Integer authorId = review.getUserId();

        //load user who created the review
        User author = userrepository.findById(authorId).orElse(null);
        if(author == null){
            return; //return if the user does not exist
        }

        //get a list of User objects  who follow this author
        List<User> followers = followrepository.getFollowersOf(authorId);

        //if no followers, no notifications need to be sent
        if(followers.isEmpty()){
            return;
        }

        List<Notification> notifs = new ArrayList<>();

        for(User follower: followers){
            Notification n = new Notification();
            n.setRecipient(follower);
            n.setActor(author);
            n.setType("REVIEW");
            n.setMessage(author.getFirstname() + " " + author.getLastname()  + " posted a new review!");
            n.setCreatedAt(LocalDateTime.now());

            notifs.add(n);
        }
        //save to DB
        List<Notification> saved = notificationrepository.saveAll(notifs);
        //push WS
        for(Notification n : saved){
            push(n);
        }
    }

    /**
     * Notifies the followed user about the new follower
     * @param follower (user who just followed)
     * @param followed (user now being followed)
     */
    @Transactional
    public void notifyUserFollow(User follower, User followed){
        //create a notification for the followed user
        Notification n = new Notification();
        n.setActor(follower);
        n.setRecipient(followed);
        n.setType("FOLLOW");
        n.setMessage(follower.getEmail() + " followed you.");
        n.setCreatedAt(java.time.LocalDateTime.now());
        n.setReadAt(null);

        //save to DB
        Notification saved = notificationrepository.save(n);
        //push to WS
        push(saved);
    }

    @Transactional
    public void notifyFollowersOfNewList(UserList list){
        if(list == null ||list.getPrivacy() != UserList.Privacy.PUBLIC) return;

        User owner = list.getOwner();
        if(owner == null || owner.getId() == null) return;

        List<User> followers = followrepository.getFollowersOf(owner.getId());
        if(followers.isEmpty()) return;

        List<Notification> notifs = new ArrayList<>();
        for(User follower : followers){
            Notification n = new Notification();
            n.setRecipient(follower);
            n.setActor(owner);
            n.setType("LIST");
            n.setMessage(owner.getFirstname() + " " + owner.getLastname() + " created a new list: " + list.getTitle());
            n.setCreatedAt(LocalDateTime.now());
            notifs.add(n);
        }
        //save to DB
        List<Notification> saved = notificationrepository.saveAll(notifs);

        //push WS
        for(Notification n : saved){
            push(n);
        }
    }

    /**
     * Pushes the notification
     * @param n notification to send to user
     */
    public void push(Notification n){
        try{
            NotificationEndpoint.sendToUser(
                    n.getRecipient().getId(),
                        "{\"event\":\"NOTIFICATION\",\"notifId\":" + n.getNotifId() + "}");
            }catch(Exception ignored){
            // WS send failed (user offline/closed). Notification is still stored in DB.
            }
    }

    /**
     * Notifies a user when they receive a recommendation from another user
     * @param sender the user sending the recommendation
     * @param recipient the user receiving the recommendation
     * @param title the title of the recommended item
     */
    @Transactional
    public void notifyUserRecommendation(User sender, User recipient, String title ){
        //don't notify if rec is for themselves
        if(sender.getId().equals(recipient.getId())){
            return;
        }

        Notification n = new Notification();
        n.setActor(sender);
        n.setRecipient(recipient);
        n.setType("RECOMMENDATION");
        n.setMessage(sender.getFirstname() + " " + sender.getLastname() + " recommended \"" + title + "\" to you!");
        n.setCreatedAt(LocalDateTime.now());
        n.setReadAt(null);

        Notification saved = notificationrepository.save(n);

        push(saved);

    }


}
