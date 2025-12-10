package com.imp3.Backend.chat;

import com.imp3.Backend.user.User;
import com.imp3.Backend.user.UserRepository;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value="/chat/{userId}")
public class ChatEndpoint {

    // ============================================================
    // Setup
    // ============================================================

    // for associating chatters with their socket sessions
    private static final Map<Integer, Session> userSessionMap = new ConcurrentHashMap<>();
    private static final Map<Session, Integer> sessionUserMap = new ConcurrentHashMap<>();

    // server side logger
    private final Logger logger = LoggerFactory.getLogger(ChatEndpoint.class);

    // to make sure senders/receivers exist
    private static UserRepository userrepository;
    static void attachUserRepo(UserRepository r) { userrepository = r; }

    // attach the chat repository too (seems more complicated than normal Controller classes)
    private static ChatRepository chatrepository;
    static void attachChatRepo(ChatRepository r) { chatrepository = r; }

    // ============================================================
    // On Open
    // ============================================================

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Integer userId) throws IOException {

        // check if user actually exists
        User user = userrepository.findById(userId).orElse(null);
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "User " + userId + " not found"));
            return;
        }

        // server side log
        logger.info("[onOpen] {}", userId);

        // map user with session
        userSessionMap.put(userId, session);

        // map session with user
        sessionUserMap.put(session, userId);

        // send to the user joining in (for testing)
        sendChatToUser(userId, userId, "You are online, " + userId);

    }

    // ============================================================
    // On Close
    // ============================================================

    @OnClose
    public void onClose(Session session) throws IOException {

        Integer userId = sessionUserMap.get(session);

        sessionUserMap.remove(session);
        userSessionMap.remove(userId);

        logger.info("[onClose] {}", userId);
    }

    // ============================================================
    // On Error
    // ============================================================

    @OnError
    public void onError(Session session, Throwable thrown) throws IOException {
        Integer userId = sessionUserMap.get(session);
        logger.error("[onError] user = {} : {}", userId, thrown.getMessage(), thrown);
    }

    // ============================================================
    // On Message
    // ============================================================

    @OnMessage
    public void OnMessage(Session session, String chat) throws IOException {

        logger.info("Beginning onMessage({})", chat);

        // get the sender's userId
        Integer sender = sessionUserMap.get(session);

        // split chat into receiver and message
        int commaIndex = chat.indexOf(',');
        if (commaIndex == -1) {
            logger.warn("Malformed message");
            session.getBasicRemote().sendText("Error: bad message formatting");
            return;
        }

        String receiverString = chat.substring(0, commaIndex).trim();
        String messageString = chat.substring(commaIndex + 1).trim();

        // parse the receiver id string into an integer
        Integer receiver;
        try {
            receiver = Integer.parseInt(receiverString);
        } catch (NumberFormatException e) {
            session.getBasicRemote().sendText("Error: bad receiver ID number");
            return;
        }

        // send the chat out
        sendChatToUser(sender, receiver, messageString);
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    private void sendChatToUser(Integer sender, Integer receiver, String message) {

        logger.info("Beginning sendChatToUser({}, {}, {})", sender, receiver, message);

        // make a new chat
        Chat chat = new Chat();

        // populate the values
        chat.setSender(sender);
        chat.setReceiver(receiver);
        chat.setMessage(message);
        chat.setUnread(true);

        // send the message to the sender and the receiver
        try {

            // message will appear on both user's screens
            userSessionMap.get(receiver).getBasicRemote().sendText(sender + ": " + message);

            // unless they sent it to themselves
            if (!sender.equals(receiver)) {
                userSessionMap.get(sender).getBasicRemote().sendText(sender + ": " + message);
            }

        } catch (IOException e) {
            logger.info(e.getMessage());
        }

        // save the message to the database
        chatrepository.save(chat);

    }
}
