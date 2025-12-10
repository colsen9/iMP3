package com.imp3.Backend.notification;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value="/ws/notif/{uid}")
public class NotificationEndpoint {

    private static final Map<Integer, Session> userSessionMap = new ConcurrentHashMap<>();
    private static final Map<Session, Integer> sessionUserMap = new ConcurrentHashMap<>();

    //service side logger
    private final Logger logger = LoggerFactory.getLogger(NotificationEndpoint.class);

    /**
     * OnOpen - handles a WebSocket connection when a user connects to the server
     * @param session the active WebSocket session for connected user
     * @param stringUid the unique ID of the user connecting (as a String)
     * @throws IOException if an I/O error occurs while sending or receiving data
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("uid") String stringUid) throws IOException {
        logger.info("[onOpen] User connected: {}",  stringUid);
        int uid = 0;
        try{
            uid = Integer.parseInt(stringUid);
        } catch (NumberFormatException e){
            logger.info(e.getMessage());
        }
        //track user and session
        userSessionMap.put(uid, session);
        sessionUserMap.put(session, uid);
    }

    /**
     * OnClose - handles a WebSocket connection when a user disconnects from the server
     * @param session the active WebSocket session for the user who disconnected
     * @throws IOException if an I/O error occurs while sending or receiving data
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        Integer uid = sessionUserMap.get(session);

        if(uid != null){
            userSessionMap.remove(uid);
            sessionUserMap.remove(uid);
            logger.info("[onClose] User disconnected: {}", uid);
        } else{
            logger.info("[onClose] Unknown session disconnected.");
        }
        session.close();
    }

    /**
     * OnError - called when a WebSocket error occurs for a given session
     * Logs the error and removes the session from maps to avoid leaks
     * @param session the active WebSocket session for the user who disconnected
     * @param throwable the exception or error that was thrown
     */
    @OnError
    public void onError(Session session, Throwable throwable){
        Integer uid = sessionUserMap.get(session);

        if(uid != null){
            userSessionMap.remove(uid);
            sessionUserMap.remove(uid);
            logger.error("[onError] Error on session for user {}: {}", uid,throwable.getMessage(), throwable);
        } else{
            sessionUserMap.remove(session);
            logger.error("[onError] Error on unknown session: {}", throwable.getMessage(), throwable);
        }

        try{
            if(session != null && session.isOpen()){
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "WebSocket error"));
            }
        } catch(IOException except){
            logger.warn("[onError] Failed to close errored session: {}", except.getMessage(), except);
        }
    }

    /**
     * Sends a notification to a specific user
     * @param uid of the user
     * @param message to send
     * @throws IOException if an I/O error occurs while sending or receiving data
     */
    public static void sendToUser(Integer uid, String message) throws IOException{
        Session userSession = userSessionMap.get(uid);
        if(userSession != null && userSession.isOpen()){
            userSession.getBasicRemote().sendText(message);
        }
    }

}
