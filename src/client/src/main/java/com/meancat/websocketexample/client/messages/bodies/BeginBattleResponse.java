package com.meancat.websocketexample.client.messages.bodies;

public class BeginBattleResponse {
    public boolean successful;
    public Result result;

    public enum Result {
        OK,
        INVALID_REQUEST
        // etc, etc etc...
    }

}
