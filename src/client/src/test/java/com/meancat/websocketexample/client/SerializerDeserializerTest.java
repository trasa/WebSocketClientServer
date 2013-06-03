package com.meancat.websocketexample.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meancat.websocketexample.client.messages.Message;
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

}
