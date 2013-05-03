package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meancat.websocketexample.client.messages.Message;
import org.eclipse.jetty.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class WebSocketClientImpl implements WebSocketClient, WebSocket.OnTextMessage {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientImpl.class);

    @Value("${service.url}")
    protected String serviceUrl;

    @Autowired
    protected org.eclipse.jetty.websocket.WebSocketClient client;

    @Autowired
    protected ObjectMapper objectMapper;


    protected WebSocket.Connection connection;
    private String connectedUrl;

    @Override
    public void connect(String url) throws WebSocketException {
        try {
            Future<WebSocket.Connection> future = client.open(new URI(url), this);
            connection = future.get(5, TimeUnit.SECONDS);
            connectedUrl = url;
        } catch (Exception e) {
            connectedUrl = null;
            throw new WebSocketException(e);
        }
    }

    @Override
    public void connect() throws WebSocketException {
        connect(serviceUrl);
    }

    @Override
    public String getConnectedUrl() {
        return connectedUrl;
    }

    @Override
    public void sendPingWebSocketFrame() {
        // Don't have this particular implementation exposed, since
        // we're only handling OnTextMessage.
        throw new NotImplementedException();
    }

    @Override
    public void onMessage(String data) {
        // a message is coming in!
        logger.debug("message received: {}", data);
        try {
            Message message = stringToMessage(data);
            handleMessage(message);
        } catch (IOException e) {
            logger.error("Failed to parse incoming message! " + data, e);
        }
    }

    private void handleMessage(Message message) {
        // here, we would do something interesting with finding the right place to route this message to,
        // execute whatever the message was about, tell the client new information, or whatever.
        //
        // for this example, we don't do any of that.
        logger.info("Message Received and handled: {}", message.text);
    }

    @Override
    public void send(Message message) {
        // a message is going out!
        String data = null;
        try {
            data = messageToString(message);
            connection.sendMessage(data);
        } catch (Exception e) {
            logger.error("Failed to send message! " + data, e);
        }
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }


    @Override
    public void onOpen(Connection connection) {
        logger.info("connection open!");
    }

    @Override
    public void onClose(int closeCode, String message) {
        logger.info("connection closed! code {}, message {}", closeCode,  message);
        connectedUrl =  null;
    }

    private Message stringToMessage(String data) throws IOException {
        return objectMapper.readValue(data, Message.class);
    }

    private String messageToString(Message message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }
}
