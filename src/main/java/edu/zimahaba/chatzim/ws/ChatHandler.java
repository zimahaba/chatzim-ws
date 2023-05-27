package edu.zimahaba.chatzim.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.zimahaba.chatzim.model.ChatMessage;
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

    private static String CHANNEL_KEY = "channel";
    private static String USER_KEY = "userId";

    Map<String, List<UserSession>> channels = new ConcurrentHashMap<>();

    private final ObjectMapper om;

    public ChatHandler(ObjectMapper om) {
        this.om = om;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage chatMessage = om.readValue(message.getPayload(), ChatMessage.class);
        if (chatMessage.isConnect()) {
            handleConnection(session, chatMessage);
        } else if (chatMessage.isChat()) {
            handleMessage(session, chatMessage);
        }
    }

    private void handleConnection(WebSocketSession session, ChatMessage chatMessage) throws IOException {
        String channel = chatMessage.getChannel();
        String userId = chatMessage.getUserId();
        if (hasText(channel) && hasText(userId)) {
            log.info("Connection established with user '{}' in channel '{}'", userId, channel);

            session.getAttributes().put(CHANNEL_KEY, channel);
            session.getAttributes().put(USER_KEY, userId);

            List<UserSession> userSessions;
            if (channels.containsKey(channel)) {
                userSessions = channels.get(channel);
            } else {
                userSessions = new ArrayList<>();
            }

            userSessions.add(new UserSession(userId, session));
            channels.put(channel, userSessions);

            String connectionResponse;
            try {
                connectionResponse = om.writeValueAsString(new ChatMessage("connection", channel, userId));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            //String connectionResponse = "{\"method\": \"connection\", \"username\":\"" + userId + "\"}";

            log.info("Sending connection response: {}", connectionResponse);
            session.sendMessage(new TextMessage(connectionResponse));
        }
    }

    private void handleMessage(WebSocketSession session, ChatMessage chatMessage) {
        //log.info("Message received from '{}': {}", userId, message.getPayload());
        String channel = (String) session.getAttributes().get(CHANNEL_KEY);
        String userId = (String) session.getAttributes().get(USER_KEY);
        String payload = chatMessage.getPayload();

        String broadcastPayload;
        try {
            broadcastPayload = om.writeValueAsString(new ChatMessage("chat", channel, userId, payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        //String broadcastPayload = "{\"method\": \"chat\", \"username\":\"" + userId + "\", \"payload\":\"" + payload + "\"}";
        channels.get(channel).forEach((userSession -> {
            if (!userSession.getUserId().equals(userId)) {
                log.info("Got user '{}', sending msg: {}", userId, payload);
                try {
                    userSession.getWsSession().sendMessage(new TextMessage(broadcastPayload));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("Transport error");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Connection closed");
        String channel = (String) session.getAttributes().get(CHANNEL_KEY);
        String userId = (String) session.getAttributes().get(USER_KEY);
    }
}
