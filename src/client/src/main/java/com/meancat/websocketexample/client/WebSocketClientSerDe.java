package com.meancat.websocketexample.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.meancat.websocketexample.client.messages.Message;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Serializer-Deserializer for Messages coming to/going from our service
 */
@Component
public class WebSocketClientSerDe extends SimpleChannelHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientSerDe.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();

        if (msg instanceof WebSocketFrame || msg instanceof HttpRequest) {
            // send this down as-is
            ctx.sendDownstream(e);
            return;
        }

        Message message = (Message) e.getMessage();

        // convert the Message into json and send that as a TextWebSocketFrame instead.
        ChannelBuffer response = ChannelBuffers.wrappedBuffer(messageToBytes(message));
        ctx.getChannel().write(new TextWebSocketFrame(response));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        logger.debug("message received: {}", e.getMessage());
        Object msg = e.getMessage();

        if (msg instanceof TextWebSocketFrame) {
            // deserialize the frame into a Message
            handleTextWebSocketFrame(ctx, e, (TextWebSocketFrame) msg);
        } else {
            // ignore this and send it as-is
            ctx.sendUpstream(e);
        }
    }

    protected byte[] messageToBytes(Message message) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(message);
        if (logger.isDebugEnabled()) {
            logger.debug("resulted encoding is: {}", new String(bytes, Charset.defaultCharset()));
        }
        return bytes;
    }

    protected Message bytesToMessage(ChannelBuffer buf) throws IOException {
        return bytesToMessage(buf.toString(CharsetUtil.UTF_8));
    }

    protected Message bytesToMessage(String frameData) throws IOException {
        return objectMapper.readValue(frameData, Message.class);
    }

    /**
     * Handles a TextWebSocketFrame.
     *
     * @param ctx   context
     * @param e     MessageEvent
     * @param frame frame
     */
    private void handleTextWebSocketFrame(ChannelHandlerContext ctx, MessageEvent e, TextWebSocketFrame frame) {

        Channel channel = ctx.getChannel();

        try {
            Message message = bytesToMessage(frame.getBinaryData());
            logger.debug("Handling message {} to {}", message, e.getRemoteAddress());
            ctx.sendUpstream(new UpstreamMessageEvent(channel, message, e.getRemoteAddress()));
        } catch (Exception ex) {
            logger.error(
                    String.format("Received invalid message from server: %s", frame.getBinaryData().toString(CharsetUtil.UTF_8)),
                    ex);

        }
    }
}
