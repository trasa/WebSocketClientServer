package com.meancat.websocketexample.client;

import com.google.common.base.Strings;
import com.meancat.websocketexample.client.messages.Message;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handles the interface to our netty websocket pipeline.
 */
@Component
public class WebSocketClientHandler extends SimpleChannelUpstreamHandler implements WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    @Value("${service.url}")
    protected String serviceUrl;

    @Autowired
    protected WebSocketClientHandshaker handshaker;
    @Autowired
    protected ChannelPipelineFactory socketChannelPipelineFactory;
    @Autowired
    protected ChannelFactory socketChannelFactory;

    protected ClientBootstrap clientBootstrap;

    private Channel serviceChannel;
    private String connectedUrl;

    @PostConstruct
    public void init() {
        clientBootstrap = new ClientBootstrap(socketChannelFactory);
        clientBootstrap.setPipelineFactory(socketChannelPipelineFactory);
    }

    @PreDestroy
    public void destroy() {
        close();
        if (clientBootstrap != null) {
            clientBootstrap.releaseExternalResources();
        }
    }


    @Override
    public void connect(String url) throws WebSocketException {
        if (serviceChannel != null && serviceChannel.isConnected()) {
            logger.warn("called connect() but the channel is already connected!");
            // TODO do something useful in this case :)
            return;
        }
        if (Strings.isNullOrEmpty(url)) {
            throw new WebSocketException("Can't connect, url is null or empty");
        }
        logger.info("Connecting to {}...", url);
        try {
            URI uri = new URI(url);
            ChannelFuture future = clientBootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
            future.syncUninterruptibly();
            Channel ch = future.getChannel();
            handshaker.handshake(ch).syncUninterruptibly();
            this.serviceChannel = ch;
            connectedUrl = url;
        } catch (URISyntaxException e) {
            throw new WebSocketException("Failed to parse url:" + url, e);
        } catch (Exception e) {
            throw new WebSocketException("Failed to connect to " + url, e);
        }
    }

    @Override
    public void connect() throws WebSocketException {
        connect(serviceUrl);
    }

    @Override
    public void sendPingWebSocketFrame() {
        serviceChannel.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));
    }

    @Override
    public void send(Message message) {
        serviceChannel.write(message);
    }

    @Override
    public void close() {
        if (serviceChannel == null) {
            logger.warn("serviceChannel is null, but close() was called");
            return;
        }
        if (serviceChannel.isOpen()) {
            logger.info("Closing serviceChannel");
            serviceChannel.write(new CloseWebSocketFrame());
            serviceChannel.getCloseFuture().syncUninterruptibly();
            serviceChannel.close();
        }
        connectedUrl = null;
        serviceChannel = null;
    }

    @Override
    public boolean isConnected() {
        return serviceChannel != null && serviceChannel.isConnected();
    }

    @Override
    public String getConnectedUrl() {
        return connectedUrl;
    }


    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
        logger.info("Channel connected [{}]", e.getChannel());
    }


    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        logger.info("channel closed!");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        logger.info("Channel disconnected [{}]", e.getChannel());
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Channel ch = ctx.getChannel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
            logger.info("websocket connected!!");
            return;
        }

        if (e.getMessage() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) e.getMessage();
            throw new WebSocketException(
                    String.format("Unexpected HttpResponse (status=%s, content=%s)",
                            response.getStatus(), response.getContent().toString(CharsetUtil.UTF_8)));
        }

        Object frame = e.getMessage();

        if (frame instanceof Message) {
            handleMessage((Message) frame);
        } else if (frame instanceof TextWebSocketFrame) {
            handleTextWebSocketFrame((TextWebSocketFrame) frame);
        } else if (frame instanceof PongWebSocketFrame) {
            handlePong((PongWebSocketFrame) frame);
        } else if (frame instanceof CloseWebSocketFrame) {
            logger.debug("websocket client received closing!");
            ch.close();
        } else {
            logger.warn("I dont know how to handle this type of message event: {} - {}", frame.getClass().getName(), frame.toString());
        }
    }


    // lets handle some incoming messages:
    private void handleTextWebSocketFrame(TextWebSocketFrame frame) {
        logger.info("text frame: {}", frame.getText());
    }

    private void handlePong(PongWebSocketFrame pong) {
        logger.info("Pong: {}", pong);
    }


    /**
     * Handle the game message.
     */
    protected void handleMessage(Message message) throws Exception {
        // here, we would do something interesting with finding the right place to route this message to,
        // execute whatever the message was about, tell the client new information, or whatever.
        //
        // for this example, we don't do any of that.
        logger.info("Message Received: {}", message.text);
    }
}
