package edu.zimahaba.chatzim.ws;

import edu.zimahaba.chatzim.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.util.StringUtils.hasText;


public class ChatHandler extends TextWebSocketHandler {

    Logger log = LoggerFactory.getLogger(ChatHandler.class);

    private static String CHANNEL_HEADER = "channel";
    private static String USER_ID_HEADER = "userId";

    Map<String, List<UserSession>> channels = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String channel =  session.getHandshakeHeaders().getFirst(CHANNEL_HEADER);
        String userId =  session.getHandshakeHeaders().getFirst(USER_ID_HEADER);
        if (hasText(channel) && hasText(userId)) {
            log.info("Connection established with user '{}' in channel '{}'", userId, channel);

            // TODO move set attributes to handshake interceptor?
            session.getAttributes().put(CHANNEL_HEADER, channel);
            session.getAttributes().put(USER_ID_HEADER, userId);

            List<UserSession> userSessions;
            if (channels.containsKey(channel)) {
                userSessions = channels.get(channel);
            } else {
                userSessions = new ArrayList<>();
            }

            userSessions.add(new UserSession(userId, session));
            channels.put(channel, userSessions);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String channel = (String) session.getAttributes().get(CHANNEL_HEADER);
        String userId =  (String) session.getAttributes().get(USER_ID_HEADER);

        if (hasText(channel) && hasText(userId) && channels.containsKey(channel)) {
            //log.info("Message received from '{}': {}", userId, message.getPayload());
            channels.get(channel).forEach((userSession -> {
                if (!userSession.getUserId().equals(userId)) {
                    //log.info("Got user '{}', sending msg: {}", userId, message.getPayload());
                    try {
                        userSession.getWsSession().sendMessage(new TextMessage(message.getPayload()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("Transport error");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Connection closed");
    }
}
