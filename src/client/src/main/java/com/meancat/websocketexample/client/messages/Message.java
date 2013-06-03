package com.meancat.websocketexample.client.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.meancat.websocketexample.client.messages.bodies.BeginBattleRequest;
import com.meancat.websocketexample.client.messages.bodies.BeginBattleResponse;
import com.meancat.websocketexample.client.messages.bodies.GetNameRequest;
import com.meancat.websocketexample.client.messages.bodies.GetNameResponse;

public class Message {

    public String text;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="bodyType")
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = BeginBattleRequest.class),
            @JsonSubTypes.Type(value = BeginBattleResponse.class),
            @JsonSubTypes.Type(value = GetNameRequest.class),
            @JsonSubTypes.Type(value = GetNameResponse.class)
    })
    public Object body;

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }
}
