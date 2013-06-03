package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meancat.websocketexample.client.messages.Message;
import com.meancat.websocketexample.client.messages.MessageBody;
import com.meancat.websocketexample.client.messages.bodies.BeginBattleRequest;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SerializerDeserializerTest {

    private Reflections reflections;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder()
                        .include(FilterBuilder.prefix("com.meancat.websocketexample.client")))
                .setUrls(ClasspathHelper.forPackage("com.meancat.websocketexample.client"))
                .setScanners(new SubTypesScanner(),
                        new TypeAnnotationsScanner(),
                        new ResourcesScanner()));

        objectMapper = new ObjectMapper(new JsonFactory());
        objectMapper.disableDefaultTyping();
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);

        // register our @MessageBody classes with the objectMapper:
        for(Class<?> clazz : reflections.getTypesAnnotatedWith(MessageBody.class)) {
            objectMapper.registerSubtypes(clazz);
        }
    }

    @Test
    public void BeginBattleRequest_serialize() throws JsonProcessingException {
        BeginBattleRequest request = new BeginBattleRequest();
        request.opponentName = "thatGuyOverThere";
        Message message = new Message("fight!");
        message.body = request;

        String json = objectMapper.writeValueAsString(message);
        System.out.println(json);
    }

    @Test
    public void BeginBattleRequest_deserialize() throws IOException {
        String json = "{\"text\":\"fight!\",\"body\":{\"bodyType\":\"BeginBattleRequest\",\"opponentName\":\"thatGuyOverThere\"}}";

        Message message = objectMapper.readValue(json, Message.class);
        assertEquals(BeginBattleRequest.class, message.body.getClass());
        assertEquals("thatGuyOverThere", ((BeginBattleRequest)message.body).opponentName);
    }
}
