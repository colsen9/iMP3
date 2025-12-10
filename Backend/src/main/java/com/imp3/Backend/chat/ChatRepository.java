package com.imp3.Backend.chat;

import com.imp3.Backend.chat.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    // Find Chats
    List<Chat> findBySender(Integer sender);
    List<Chat> findByReceiver(Integer receiver);

}