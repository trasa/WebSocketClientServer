package com.meancat.websocketexample.client.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class Message {

    public String text;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="bodyType")
    // Instead we'll define these values at runtime:
//    @JsonSubTypes(value = {
//            @JsonSubTypes.Type(value = BeginBattleRequest.class),
//            @JsonSubTypes.Type(value = BeginBattleResponse.class),
//            @JsonSubTypes.Type(value = GetNameRequest.class),
//            @JsonSubTypes.Type(value = GetNameResponse.class)
//    })
    public Object body;

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }
}
