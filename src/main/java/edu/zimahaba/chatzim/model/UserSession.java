package edu.zimahaba.chatzim.model;

import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

public class UserSession {

    public UserSession(String userId, WebSocketSession session) {
        this.userId = userId;
        this.wsSession = session;
    }

    private String userId;
    private WebSocketSession wsSession;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public WebSocketSession getWsSession() {
        return wsSession;
    }

    public void setWsSession(WebSocketSession wsSession) {
        this.wsSession = wsSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return userId.equals(that.userId) && wsSession.equals(that.wsSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, wsSession);
    }
}
