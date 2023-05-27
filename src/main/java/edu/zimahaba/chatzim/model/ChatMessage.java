package edu.zimahaba.chatzim.model;

import java.util.Objects;

public class ChatMessage {

    private String method;
    private String channel;
    private String userId;
    private String payload;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ChatMessage() {
    }

    public ChatMessage(String method, String channel, String userId) {
        this.method = method;
        this.channel = channel;
        this.userId = userId;
    }

    public ChatMessage(String method, String channel, String userId, String payload) {
        this.method = method;
        this.channel = channel;
        this.userId = userId;
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return method.equals(that.method) && channel.equals(that.channel) && userId.equals(that.userId) && payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, channel, userId, payload);
    }

    public boolean isConnect() {
        return this.method.equals("connect");
    }

    public boolean isChat() {
        return this.method.equals("chat");
    }
}
