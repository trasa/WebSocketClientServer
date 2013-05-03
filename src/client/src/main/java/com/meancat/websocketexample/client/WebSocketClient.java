package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meancat.websocketexample.client.messages.Message;

public interface WebSocketClient {
    void connect(String url) throws WebSocketException;

    void connect() throws WebSocketException;

    void sendPingWebSocketFrame();

    void send(Message message);

    void close();

    boolean isConnected();

    String getConnectedUrl();
}
