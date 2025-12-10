package com.imp3.Backend.chat;

import com.imp3.Backend.music.AlbumDTO;
import com.imp3.Backend.music.MusicMapper;
import com.imp3.Backend.notification.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("chat")
public class ChatController {

    // ============================================================
    // Repos and DTOs
    // ============================================================

    @Autowired
    ChatRepository chatrepository;

    // ============================================================
    // GET Methods
    // ============================================================

    // get all chat messages sent by a user
    @GetMapping("{userId}/sent")
    public List<Chat> getAllChatsSender(@PathVariable Integer userId) {
        return chatrepository.findBySender(userId);
    }

    // get all chat messages received by a user
    @GetMapping("{userId}/received")
    public List<Chat> getAllChatsReceiver(@PathVariable Integer userId) {
        return chatrepository.findByReceiver(userId);
    }

    // ============================================================
    // POST Methods
    // ============================================================

    @PostMapping("/")
    public Chat postChat(@RequestBody Chat chat) {

        // save the chat
        chatrepository.save(chat);

        // don't really need this...
        return chat;
    }

    // ============================================================
    // PUT Methods
    // ============================================================

    @PutMapping("/")
    public Chat editChat(@RequestBody Chat chat) {

        // save the chat
        chatrepository.save(chat);

        // don't really need this...
        return chat;
    }

    // ============================================================
    // DELETE Methods
    // ============================================================

    @DeleteMapping("/")
    public Chat deleteChat(@RequestBody Chat chat) {

        // save the chat
        chatrepository.delete(chat);

        // don't really need this...
        return chat;
    }

}
