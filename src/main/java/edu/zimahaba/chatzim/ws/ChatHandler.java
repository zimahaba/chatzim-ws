package edu.zimahaba.chatzim.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        /*String channel =  session.getHandshakeHeaders().getFirst(CHANNEL_KEY);
        String userId =  session.getHandshakeHeaders().getFirst(USER_KEY);
        if (hasText(channel) && hasText(userId)) {
            log.info("Connection established with user '{}' in channel '{}'", userId, channel);

            // TODO move set attributes to handshake interceptor?
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
        }*/
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonPayload = om.readTree(message.getPayload());
        String method = jsonPayload.get("method").asText();

        if (method.equals("connect")) {
            handleConnection(session, jsonPayload);
        } else if (method.equals("chat")) {
            handleMessage(session, jsonPayload);
        }
    }

    private void handleConnection(WebSocketSession session, JsonNode jsonPayload) throws IOException {
        String channel = jsonPayload.get(CHANNEL_KEY).asText();
        String userId = jsonPayload.get(USER_KEY).asText();
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
            String connectionResponse = "{\"method\": \"connection\", \"username\":\"" + userId + "\"}";
            log.info("Sending connection response: {}", connectionResponse);
            session.sendMessage(new TextMessage(connectionResponse));
        }
    }

    private void handleMessage(WebSocketSession session, JsonNode jsonPayload) {
        //log.info("Message received from '{}': {}", userId, message.getPayload());
        String channel = (String) session.getAttributes().get(CHANNEL_KEY);
        String userId = (String) session.getAttributes().get(USER_KEY);
        String message = jsonPayload.get("payload").asText();

        String broadcastPayload = "{\"method\": \"chat\", \"username\":\"" + userId + "\", \"message\":\"" + message + "\"}";
        channels.get(channel).forEach((userSession -> {
            if (!userSession.getUserId().equals(userId)) {
                log.info("Got user '{}', sending msg: {}", userId, message);
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
    }
}
