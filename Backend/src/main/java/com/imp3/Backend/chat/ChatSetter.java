package com.imp3.Backend.chat;

import com.imp3.Backend.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class ChatSetter {
    public ChatSetter(UserRepository userRepo, ChatRepository chatRepo) {
        ChatEndpoint.attachUserRepo(userRepo);
        ChatEndpoint.attachChatRepo(chatRepo);
    }
}